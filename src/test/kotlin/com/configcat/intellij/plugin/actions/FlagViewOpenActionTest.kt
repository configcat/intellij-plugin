package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.EnvironmentsApi
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

class FlagViewOpenActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel
    private lateinit var mockEnvironmentsApi: EnvironmentsApi
    private val productId = UUID.randomUUID()
    private val configId = UUID.randomUUID()
    private val settingId = 98765

    override fun setUp() {
        super.setUp()
        val mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.configcat.com"
        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState
        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig
        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs
        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any<ApiException>()) } just Runs
        mockSettingsPanel = mockk(relaxed = true)
        mockEnvironmentsApi = mockk(relaxed = true)
        mockkObject(ConfigCatService.Companion)
        every { ConfigCatService.createEnvironmentsService(any(), any()) } returns mockEnvironmentsApi
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testGetActionUpdateThread_returnsBGT() {
        assertEquals(ActionUpdateThread.BGT, FlagViewOpenAction().actionUpdateThread)
    }

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        FlagViewOpenAction().actionPerformed(event)
        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open Feature Flag") }) }
    }

    fun testActionPerformed_nonFlagNode_notifiesError() {
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, DefaultMutableTreeNode("x"), configModel)
        FlagViewOpenAction().actionPerformed(event)
        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open Feature Flag") }) }
    }

    fun testActionPerformed_nullConfigModel_notifiesError() {
        val event = ActionTestFixtures.createSettingsEvent(
            mockSettingsPanel,
            ActionTestFixtures.createFlagTreeNode(settingId, configId, key = "flag_key", hint = "", rootName = "Test"),
            null
        )
        FlagViewOpenAction().actionPerformed(event)
        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open Feature Flag") }) }
    }

    fun testActionPerformed_apiException_notifiesError() {
        every { mockEnvironmentsApi.getEnvironments(productId) } throws ApiException(401, "Unauthorized")
        val event = ActionTestFixtures.createSettingsEvent(
            mockSettingsPanel,
            ActionTestFixtures.createFlagTreeNode(settingId, configId, key = "flag_key", hint = "", rootName = "Test"),
            ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2)
        )
        FlagViewOpenAction().actionPerformed(event)
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testUpdate_nullSelectedNode_isDisabled() {
        val presentation = Presentation()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        every { event.presentation } returns presentation
        FlagViewOpenAction().update(event)
        assertFalse(presentation.isEnabled)
        assertTrue(presentation.isVisible)
    }

    fun testUpdate_validFlagNodeWithConfig_isEnabled() {
        val presentation = Presentation()
        val event = ActionTestFixtures.createSettingsEvent(
            mockSettingsPanel,
            ActionTestFixtures.createFlagTreeNode(settingId, configId, key = "flag_key", hint = "", rootName = "Test"),
            ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2)
        )
        every { event.presentation } returns presentation
        FlagViewOpenAction().update(event)
        assertTrue(presentation.isEnabled)
        assertTrue(presentation.isVisible)
    }

    fun testUpdate_validFlagNodeWithoutConfig_isDisabled() {
        val presentation = Presentation()
        val event = ActionTestFixtures.createSettingsEvent(
            mockSettingsPanel,
            ActionTestFixtures.createFlagTreeNode(settingId, configId, key = "flag_key", hint = "", rootName = "Test"),
            null
        )
        every { event.presentation } returns presentation
        FlagViewOpenAction().update(event)
        assertFalse(presentation.isEnabled)
        assertTrue(presentation.isVisible)
    }

    fun testCompanionObject_actionIdConstant() {
        assertEquals("CONFIGCAT_OPEN_FF_ACTION_ID", FlagViewOpenAction.CONFIGCAT_OPEN_FF_ACTION_ID)
    }
}
