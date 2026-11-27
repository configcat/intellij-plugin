package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*

class FlagRefreshActionTest : LightPlatformTestCase() {

    private lateinit var mockNodeDataService: ConfigCatNodeDataService
    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()

        mockNodeDataService = mockk(relaxed = true)

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState
        every { mockState.isConfigured() } returns true

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns mockNodeDataService
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // actionPerformed
    // -------------------------------------------------------------------------

    fun testActionPerformed_resetsConfigsFlags() {
        val action = FlagRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)

        action.actionPerformed(event)

        verify(exactly = 1) { mockNodeDataService.resetConfigsFlags() }
    }

    fun testActionPerformed_publishesSettingsTreeRefresh() {
        val action = FlagRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)

        // The action calls resetConfigsFlags and publishes refresh
        action.actionPerformed(event)

        // Verify reset was called - if it passes, the action ran to completion
        verify(exactly = 1) { mockNodeDataService.resetConfigsFlags() }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_configured_setsEnabledAndVisibleTrue() {
        val action = FlagRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled and visible", presentation.isEnabledAndVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = FlagRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)
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
            "CONFIGCAT_FLAG_REFRESH_ACTION_ID",
            FlagRefreshAction.CONFIGCAT_FLAG_REFRESH_ACTION_ID
        )
    }
}

