package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.content.ContentManager
import io.mockk.*

class HelpActionTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        val mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // actionPerformed
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullProject_doesNothing() {
        val action = HelpAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns null

        // Should not throw
        action.actionPerformed(event)
    }

    fun testActionPerformed_nullToolWindow_doesNothing() {
        val action = HelpAction()
        val mockProject = mockk<Project>(relaxed = true)
        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) } returns null

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(mockProject) } returns mockToolWindowManager

        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns mockProject

        // Should not throw
        action.actionPerformed(event)

        verify { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) }
    }

    fun testActionPerformed_validToolWindow_addsHelpContent() {
        val action = HelpAction()
        val mockProject = mockk<Project>(relaxed = true)

        val mockContentManager = mockk<ContentManager>(relaxed = true)
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) } returns mockToolWindow

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(mockProject) } returns mockToolWindowManager

        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns mockProject

        action.actionPerformed(event)

        verify { mockContentManager.addContent(any()) }
        verify { mockContentManager.setSelectedContent(any()) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_setsEnabledAndVisibleTrue() {
        val action = HelpAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled and visible", presentation.isEnabledAndVisible)
    }

    // -------------------------------------------------------------------------
    // companion object
    // -------------------------------------------------------------------------

    fun testCompanionObject_actionIdConstant() {
        assertEquals(
            "CONFIGCAT_HELP_ACTION_ID",
            HelpAction.CONFIGCAT_HELP_ACTION_ID
        )
    }
}

