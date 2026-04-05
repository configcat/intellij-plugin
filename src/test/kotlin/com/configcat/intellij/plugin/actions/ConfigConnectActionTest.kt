package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import io.mockk.*
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

class ConfigConnectActionTest : LightPlatformTestCase() {

    private lateinit var mockProductsConfigsPanel: ProductsConfigsPanel
    private lateinit var mockPropertiesService: ConfigCatPropertiesService

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        val mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns mockk(relaxed = true)

        mockPropertiesService = mockk(relaxed = true)
        mockkObject(ConfigCatPropertiesService.Companion)
        every { ConfigCatPropertiesService.getInstance() } returns mockPropertiesService

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
        val action = ConfigConnectAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error paths
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val action = ConfigConnectAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Connect action") }) }
    }

    fun testActionPerformed_nonConfigNode_notifiesError() {
        val action = ConfigConnectAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)

        action.actionPerformed(event)

        verify {
            ConfigCatNotifier.Notify.error(
                any(),
                match { it == "Connect action could not be executed without a selected Config Node." }
            )
        }
    }

    fun testActionPerformed_nullProject_notifiesError() {
        val action = ConfigConnectAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns null

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Connect action") }) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - success path
    // -------------------------------------------------------------------------

    fun testActionPerformed_validConfigNode_savesConfigId() {
        val configId = UUID.randomUUID()
        val action = ConfigConnectAction()
        val configTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createConfigNode(configId = configId))
        val event = createMockEventWithToolWindow(configTreeNode)

        action.actionPerformed(event)

        verify { mockPropertiesService.setConnectedConfig(configId.toString()) }
    }

    fun testActionPerformed_validConfigNode_switchesToolWindowTab() {
        val configId = UUID.randomUUID()
        val action = ConfigConnectAction()
        val configTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createConfigNode(configId = configId))

        val mockContent = mockk<Content>(relaxed = true)
        val mockContentManager = mockk<ContentManager>(relaxed = true)
        every { mockContentManager.getContent(1) } returns mockContent
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) } returns mockToolWindow

        val mockProject = mockk<Project>(relaxed = true)
        every { mockProject.getService(ProductsConfigsPanel::class.java) } returns mockProductsConfigsPanel
        every { mockProductsConfigsPanel.getSelectedNode() } returns configTreeNode

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(mockProject) } returns mockToolWindowManager

        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns mockProject

        action.actionPerformed(event)

        verify { mockContentManager.setSelectedContent(mockContent) }
    }

    fun testActionPerformed_noToolWindowContent_doesNotCrash() {
        val configId = UUID.randomUUID()
        val action = ConfigConnectAction()
        val configTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createConfigNode(configId = configId))

        val mockContentManager = mockk<ContentManager>(relaxed = true)
        every { mockContentManager.getContent(1) } returns null
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(any()) } returns mockToolWindow

        val mockProject = mockk<Project>(relaxed = true)
        every { mockProject.getService(ProductsConfigsPanel::class.java) } returns mockProductsConfigsPanel
        every { mockProductsConfigsPanel.getSelectedNode() } returns configTreeNode

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(mockProject) } returns mockToolWindowManager

        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns mockProject

        // Should not throw
        action.actionPerformed(event)

        verify { mockPropertiesService.setConnectedConfig(configId.toString()) }
        verify(exactly = 0) { mockContentManager.setSelectedContent(any()) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_nullSelectedNode_isDisabled() {
        val action = ConfigConnectAction()
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonConfigNode_isDisabled() {
        val action = ConfigConnectAction()
        val productTreeNode = DefaultMutableTreeNode(ActionTestFixtures.createProductNode())
        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, productTreeNode)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-ConfigNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validConfigNode_isEnabled() {
        val action = ConfigConnectAction()
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
            "CONFIGCAT_CONNECT_ACTION_ID",
            ConfigConnectAction.CONFIGCAT_CONNECT_ACTION_ID
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createMockEventWithToolWindow(selectedNode: DefaultMutableTreeNode?): AnActionEvent {
        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(any()) } returns null

        val event = ActionTestFixtures.createProductsConfigsEvent(mockProductsConfigsPanel, selectedNode)
        val project = event.project ?: error("Expected mocked project")

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(project) } returns mockToolWindowManager

        return event
    }
}
