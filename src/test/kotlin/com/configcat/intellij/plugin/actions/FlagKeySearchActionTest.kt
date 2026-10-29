package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import javax.swing.tree.DefaultMutableTreeNode

class FlagKeySearchActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel
    private lateinit var mockFindManager: FindManager
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

        mockSettingsPanel = mockk(relaxed = true)

        mockFindManager = mockk(relaxed = true)
        mockkStatic(FindManager::class)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getActionUpdateThread
    // -------------------------------------------------------------------------

    fun testGetActionUpdateThread_returnsBGT() {
        val action = FlagKeySearchAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error paths
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val action = FlagKeySearchAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Search Flag key") }) }
    }

    fun testActionPerformed_nonFlagNode_notifiesError() {
        val action = FlagKeySearchAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Search Flag key") }) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - success path
    // -------------------------------------------------------------------------

    fun testActionPerformed_validFlagNode_showsFindDialog() {
        val action = FlagKeySearchAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = java.util.UUID.randomUUID(),
            key = "my_flag_key",
            hint = "",
            rootName = "TestConfig"
        )

        val mockProject = mockk<Project>(relaxed = true)
        every { mockProject.getService(SettingsPanel::class.java) } returns mockSettingsPanel
        every { mockSettingsPanel.getSelectedNode() } returns flagTreeNode
        every { FindManager.getInstance(mockProject) } returns mockFindManager

        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns mockProject

        action.actionPerformed(event)

        verify { mockFindManager.showFindDialog(match<FindModel> {
            it.stringToFind == "my_flag_key" && it.isCaseSensitive && it.isFindAll
        }, any()) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_nullSelectedNode_isDisabled() {
        val action = FlagKeySearchAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonFlagNode_isDisabled() {
        val action = FlagKeySearchAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-FlagNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validFlagNode_isEnabled() {
        val action = FlagKeySearchAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = java.util.UUID.randomUUID(),
            hint = "",
            rootName = "TestConfig"
        )
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled for FlagNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = FlagKeySearchAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = java.util.UUID.randomUUID(),
            hint = "",
            rootName = "TestConfig"
        )
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)
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
            "CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID",
            FlagKeySearchAction.CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID
        )
    }

}

