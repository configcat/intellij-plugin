package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

class ConfigOpenInBrowserActionTest : LightPlatformTestCase() {

    private lateinit var mockProductsConfigsPanel: ProductsConfigsPanel
    private lateinit var mockBrowserLauncher: BrowserLauncher

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        val mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockProductsConfigsPanel = mockk(relaxed = true)

        mockBrowserLauncher = mockk(relaxed = true)
        mockkObject(BrowserLauncher)
        every { BrowserLauncher.instance } returns mockBrowserLauncher
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getActionUpdateThread
    // -------------------------------------------------------------------------

    fun testGetActionUpdateThread_returnsBGT() {
        val action = ConfigOpenInBrowserAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error paths
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val action = ConfigOpenInBrowserAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open in Dashboard") }) }
        verify(exactly = 0) { mockBrowserLauncher.open(any<String>()) }
    }

    fun testActionPerformed_nonConfigNode_notifiesError() {
        val action = ConfigOpenInBrowserAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open in Dashboard") }) }
        verify(exactly = 0) { mockBrowserLauncher.open(any<String>()) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - success path
    // -------------------------------------------------------------------------

    fun testActionPerformed_validConfigNode_opensBrowserWithCorrectUrl() {
        val productId = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val action = ConfigOpenInBrowserAction()
        val configTreeNode = DefaultMutableTreeNode(
            ActionTestFixtures.createConfigNode(productId = productId, configId = configId)
        )
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, configTreeNode)

        action.actionPerformed(event)

        val expectedUrl = "https://app.configcat.com/$productId/$configId"
        verify { mockBrowserLauncher.open(expectedUrl) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_nullSelectedNode_isDisabled() {
        val action = ConfigOpenInBrowserAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonConfigNode_isDisabled() {
        val action = ConfigOpenInBrowserAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-ConfigNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validConfigNode_isEnabled() {
        val action = ConfigOpenInBrowserAction()
        val configTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createConfigNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, configTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled for ConfigNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    // -------------------------------------------------------------------------
    // companion object
    // -------------------------------------------------------------------------

    fun testCompanionObject_actionIdConstant() {
        assertEquals(
            "CONFIGCAT_CONFIG_OPEN_IN_BROWSER_ACTION_ID",
            ConfigOpenInBrowserAction.CONFIGCAT_OPEN_CONFIG_IN_BROWSER_ACTION_ID
        )
    }
}
