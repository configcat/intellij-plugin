package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.dialogs.CreateFlagDialog
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.services.FlagViewOpenHandler
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.api.FeatureFlagsSettingsApi
import com.configcat.publicapi.java.client.model.SettingModel
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.util.UUID

class FlagCreateActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel
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

    fun testOpenCreatedFlagView_withCreatedFlagId_callsSharedHandler() {
        val action = FlagCreateAction()
        val configModel = ActionTestFixtures.createConnectedConfigModel(UUID.randomUUID(), UUID.randomUUID())
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, configModel)
        val mockFeatureFlagsSettingsApi = mockk<FeatureFlagsSettingsApi>(relaxed = true)
        val mockSetting = mockk<SettingModel>(relaxed = true)
        every { mockSetting.name } returns "My Created Flag"

        mockkObject(ConfigCatService)
        every { ConfigCatService.createFeatureFlagsSettingsService(any(), any()) } returns mockFeatureFlagsSettingsApi
        every { mockFeatureFlagsSettingsApi.getSetting(12345) } returns mockSetting

        mockkObject(FlagViewOpenHandler)
        every { FlagViewOpenHandler.openFlagView(any(), any(), any(), any(), any()) } returns true

        invokeOpenCreatedFlagView(action, configModel, 12345, event)

        verify(exactly = 1) {
            FlagViewOpenHandler.openFlagView(any(), any(), any(), 12345, "My Created Flag")
        }
    }

    fun testOpenCreatedFlagView_withoutCreatedFlagId_doesNotCallSharedHandler() {
        val action = FlagCreateAction()
        val configModel = ActionTestFixtures.createConnectedConfigModel(UUID.randomUUID(), UUID.randomUUID())
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, configModel)

        mockkObject(FlagViewOpenHandler)
        every { FlagViewOpenHandler.openFlagView(any(), any(), any(), any(), any()) } returns true

        invokeOpenCreatedFlagView(action, configModel, null, event)

        verify(exactly = 0) { FlagViewOpenHandler.openFlagView(any(), any(), any(), any(), any()) }
    }

    fun testActionPerformed_unsuccessfulCreate_doesNotOpenFlagView() {
        val action = FlagCreateAction()
        val configModel = ActionTestFixtures.createConnectedConfigModel(UUID.randomUUID(), UUID.randomUUID())
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, configModel)

        mockkObject(FlagViewOpenHandler)
        every { FlagViewOpenHandler.openFlagView(any(), any(), any(), any(), any()) } returns true

        mockkConstructor(CreateFlagDialog::class)
        every { anyConstructed<CreateFlagDialog>().showAndGet() } returns false

        suppressLogErrors {
            action.actionPerformed(event)
        }

        verify(exactly = 0) { FlagViewOpenHandler.openFlagView(any(), any(), any(), any(), any()) }
    }

    // -------------------------------------------------------------------------
    // actionPerformed - publish contract
    // -------------------------------------------------------------------------

    fun testNodeRefreshPublish_withFlagId_publishesSelectionRequest() {
        var published: Int? =  null
        var publishedCount = 0
        val connection = ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
        connection.subscribe(
            SettingsTreeChangeNotifier.TREE_REFRESH_TOPIC,
            object : SettingsTreeChangeNotifier {
                override fun notifyTreeRefresh(flagIdToSelect: Int?) {
                    published = flagIdToSelect
                    publishedCount++
                }
            }
        )
        val action = FlagCreateAction()
        invokeNodeRefreshPublish(action, 54321)

        assertEquals("notifyTreeRefresh must be called exactly once", 1, publishedCount)
        assertEquals("notifyTreeRefresh must be called with provided flagId", 54321, published)
    }

    fun testNodeRefreshPublish_withoutFlagId_publishesNull() {
        var published: Int? =  null
        var publishedCount = 0
        val connection = ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
        connection.subscribe(
            SettingsTreeChangeNotifier.TREE_REFRESH_TOPIC,
            object : SettingsTreeChangeNotifier {
                override fun notifyTreeRefresh(flagIdToSelect: Int?) {
                    published = flagIdToSelect
                    publishedCount++
                }
            }
        )
        val action = FlagCreateAction()
        invokeNodeRefreshPublish(action, null)

        assertEquals("notifyTreeRefresh must be called exactly once", 1, publishedCount)
        assertNull("published flagId must be null when not provided", published)
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    fun testUpdate_configured_setsEnabledAndVisibleTrue() {
        val action = FlagCreateAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled and visible", presentation.isEnabledAndVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = FlagCreateAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
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
            "CONFIGCAT_FLAG_CREATE_ACTION_ID",
            FlagCreateAction.CONFIGCAT_FLAG_CREATE_ACTION_ID
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun invokeNodeRefreshPublish(action: FlagCreateAction, flagId: Int?) {
        val method = FlagCreateAction::class.java.getDeclaredMethod("nodeRefreshPublish", Int::class.javaObjectType)
        method.isAccessible = true
        method.invoke(action, flagId)
    }

    private fun invokeOpenCreatedFlagView(
        action: FlagCreateAction,
        configModel: com.configcat.publicapi.java.client.model.ConfigModel,
        createdFlagId: Int?,
        event: AnActionEvent
    ) {
        val method = FlagCreateAction::class.java.getDeclaredMethod(
            "openCreatedFlagView",
            com.configcat.publicapi.java.client.model.ConfigModel::class.java,
            Int::class.javaObjectType,
            AnActionEvent::class.java
        )
        method.isAccessible = true
        method.invoke(action, configModel, createdFlagId, event)
    }

    private fun suppressLogErrors(action: () -> Unit) {
        val noOpProcessor = object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<String>,
                t: Throwable?,
            ): Set<Action> = emptySet()
        }
        LoggedErrorProcessor.executeWith<Throwable>(noOpProcessor) { action() }
    }
}
