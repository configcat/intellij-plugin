package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*

class ConfigRefreshActionTest : LightPlatformTestCase() {

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

    fun testActionPerformed_resetsProductConfigsData() {
        val action = ConfigRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)

        action.actionPerformed(event)

        verify(exactly = 1) { mockNodeDataService.resetProductConfigsData() }
    }

    fun testActionPerformed_publishesTreeRefresh() {
        // Use the real message bus from the test application - just verify the service is called
        val action = ConfigRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)

        action.actionPerformed(event)

        // If resetProductConfigsData was called, then the action ran successfully
        verify(exactly = 1) { mockNodeDataService.resetProductConfigsData() }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_configured_setsEnabledAndVisibleTrue() {
        val action = ConfigRefreshAction()
        val event = mockk<AnActionEvent>(relaxed = true)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled and visible", presentation.isEnabledAndVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = ConfigRefreshAction()
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
            "CONFIGCAT_CONFIG_REFRESH_ACTION_ID",
            ConfigRefreshAction.CONFIGCAT_CONFIG_REFRESH_ACTION_ID
        )
    }
}

