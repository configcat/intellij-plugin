package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*

class FlagCreateActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        val mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockSettingsPanel = mockk(relaxed = true)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getActionUpdateThread
    // -------------------------------------------------------------------------

    fun testGetActionUpdateThread_returnsBGT() {
        val action = FlagCreateAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error path
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullConnectedConfig_notifiesError() {
        val action = FlagCreateAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Create action") }) }
    }

    fun testActionPerformed_nullProject_notifiesError() {
        val action = FlagCreateAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        every { event.project } returns null

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Create action") }) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_setsEnabledAndVisibleTrue() {
        val action = FlagCreateAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
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
            "CONFIGCAT_FLAG_CREATE_ACTION_ID",
            FlagCreateAction.CONFIGCAT_FLAG_CREATE_ACTION_ID
        )
    }
}
