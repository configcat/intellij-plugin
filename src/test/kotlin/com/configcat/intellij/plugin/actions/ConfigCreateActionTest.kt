package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.lang.reflect.Method
import javax.swing.tree.DefaultMutableTreeNode

class ConfigCreateActionTest : LightPlatformTestCase() {

    private lateinit var mockProductsConfigsPanel: ProductsConfigsPanel
    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState
        every { mockState.isConfigured() } returns true

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockProductsConfigsPanel = mockk(relaxed = true)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getActionUpdateThread
    // -------------------------------------------------------------------------

    fun testGetActionUpdateThread_returnsBGT() {
        val action = ConfigCreateAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error paths
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val action = ConfigCreateAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Create Config") }) }
    }

    fun testActionPerformed_nonProductNode_notifiesError() {
        val action = ConfigCreateAction()
        val configTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createConfigNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, configTreeNode)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Create Config") }) }
    }

    fun testActionPerformed_nullProject_handledGracefully() {
        val action = ConfigCreateAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns null

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Create Config") }) }
    }

    fun testNodeRefreshPublish_withConfigId_publishesSelectionRequest() {
        val action = ConfigCreateAction()
        val published = arrayOfNulls<Any>(2)
        val connection = ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
        connection.subscribe(
            ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC,
            object : ProductsConfigsTreeChangeNotifier {
                override fun notifyTreeRefresh() = Unit

                override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode, configIdToSelect: String?) {
                    published[0] = node
                    published[1] = configIdToSelect
                }
            }
        )

        val node = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        invokeNodeRefreshPublish(action, node, "created-config-id")

        assertSame(node, published[0])
        assertEquals("created-config-id", published[1])
    }

    fun testNodeRefreshPublish_withoutConfigId_publishesNullSelectionRequest() {
        val action = ConfigCreateAction()
        val published = arrayOfNulls<Any>(2)
        val connection = ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
        connection.subscribe(
            ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC,
            object : ProductsConfigsTreeChangeNotifier {
                override fun notifyTreeRefresh() = Unit

                override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode, configIdToSelect: String?) {
                    published[0] = node
                    published[1] = configIdToSelect
                }
            }
        )

        val node = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        invokeNodeRefreshPublish(action, node, null)

        assertSame(node, published[0])
        assertNull(published[1])
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_nullSelectedNode_isDisabled() {
        val action = ConfigCreateAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonProductNode_isDisabled() {
        val action = ConfigCreateAction()
        val nonProductTreeNode = DefaultMutableTreeNode("not a product")
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, nonProductTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-ProductNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_productNode_isEnabled() {
        val action = ConfigCreateAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled for ProductNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = ConfigCreateAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when plugin is not configured", presentation.isEnabled)
        assertFalse("Presentation must be hidden when plugin is not configured", presentation.isVisible)
    }

    // -------------------------------------------------------------------------
    // companion object
    // -------------------------------------------------------------------------

    fun testCompanionObject_actionIdConstant() {
        assertEquals(
            "CONFIGCAT_CONFIG_CREATE_ACTION_ID",
            ConfigCreateAction.CONFIGCAT_CONFIG_CREATE_ACTION_ID
        )
    }

    private fun invokeNodeRefreshPublish(action: ConfigCreateAction, node: DefaultMutableTreeNode, configId: String?) {
        val method: Method = ConfigCreateAction::class.java.getDeclaredMethod(
            "nodeRefreshPublish",
            DefaultMutableTreeNode::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(action, node, configId)
    }
}
