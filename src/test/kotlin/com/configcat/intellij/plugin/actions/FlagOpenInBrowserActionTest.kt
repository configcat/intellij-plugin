package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.EnvironmentsApi
import com.configcat.publicapi.java.client.model.EnvironmentModel
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

class FlagOpenInBrowserActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel
    private lateinit var mockBrowserLauncher: BrowserLauncher
    private lateinit var mockEnvironmentsApi: EnvironmentsApi
    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    private val productId = UUID.randomUUID()
    private val configId = UUID.randomUUID()
    private val settingId = 12345
    private val environmentId = UUID.randomUUID()
    private val orgId = UUID.randomUUID()

    override fun setUp() {
        super.setUp()

        mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.configcat.com"
        every { mockState.isConfigured() } returns true

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) } just Runs

        mockSettingsPanel = mockk(relaxed = true)

        mockBrowserLauncher = mockk(relaxed = true)
        mockkObject(BrowserLauncher)
        every { BrowserLauncher.instance } returns mockBrowserLauncher

        mockEnvironmentsApi = mockk(relaxed = true)
        mockkObject(ConfigCatService)
        every { ConfigCatService.createEnvironmentsService(any(), any()) } returns mockEnvironmentsApi
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getActionUpdateThread
    // -------------------------------------------------------------------------

    fun testGetActionUpdateThread_returnsBGT() {
        val action = FlagOpenInBrowserAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    // -------------------------------------------------------------------------
    // actionPerformed - error paths
    // -------------------------------------------------------------------------

    fun testActionPerformed_nullSelectedNode_notifiesError() {
        val action = FlagOpenInBrowserAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open in Dashboard") }) }
        verify(exactly = 0) { mockBrowserLauncher.open(any<String>()) }
    }

    fun testActionPerformed_nonFlagNode_notifiesError() {
        val action = FlagOpenInBrowserAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, configModel)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open in Dashboard") }) }
    }

    fun testActionPerformed_nullConfigModel_notifiesError() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Open in Dashboard") }) }
    }

    fun testActionPerformed_apiException_notifiesError() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)

        every { mockEnvironmentsApi.getEnvironments(productId) } throws ApiException(500, "Server Error")

        action.actionPerformed(event)

        verify {
            ErrorHandler.errorNotify(
                any<ApiException>(),
                "Failed to get environment list. For more information check the logs.",
                any()
            )
        }
        verify(exactly = 0) { mockBrowserLauncher.open(any<String>()) }
    }

    fun testActionPerformed_emptyEnvironments_notifiesError() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)

        every { mockEnvironmentsApi.getEnvironments(productId) } returns emptyList()

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Missing information") }) }
        verify(exactly = 0) { mockBrowserLauncher.open(any<String>()) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - success path V1
    // -------------------------------------------------------------------------

    fun testActionPerformed_v1_opensBrowserWithCorrectUrl() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V1, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)

        val envModel = mockk<EnvironmentModel>(relaxed = true)
        every { envModel.environmentId } returns environmentId
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        action.actionPerformed(event)

        val expectedUrl = "https://app.configcat.com/$productId/$configId/$environmentId?settingId=$settingId"
        verify { mockBrowserLauncher.open(expectedUrl) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - success path V2
    // -------------------------------------------------------------------------

    fun testActionPerformed_v2_opensBrowserWithCorrectUrl() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)

        val envModel = mockk<EnvironmentModel>(relaxed = true)
        every { envModel.environmentId } returns environmentId
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        action.actionPerformed(event)

        val expectedUrl = "https://app.configcat.com/v2/$orgId/$productId/$configId/$environmentId/$settingId"
        verify { mockBrowserLauncher.open(expectedUrl) }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_nullSelectedNode_isDisabled() {
        val action = FlagOpenInBrowserAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonFlagNode_isDisabled() {
        val action = FlagOpenInBrowserAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, configModel)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-FlagNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validFlagNodeWithConfig_isEnabled() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled for FlagNode with config", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validFlagNodeWithoutConfig_isDisabled() {
        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when config is null", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = FlagOpenInBrowserAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(settingId, configId, hint = "", rootName = "TestConfig")
        val configModel = ActionTestFixtures.createConnectedConfigModel(productId, configId, EvaluationVersion.V2, orgId)
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, configModel)
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
            "CONFIGCAT_FLAG_OPEN_IN_BROWSER_ACTION_ID",
            FlagOpenInBrowserAction.CONFIGCAT_FLAG_OPEN_CONFIG_IN_BROWSER_ACTION_ID
        )
    }

}
