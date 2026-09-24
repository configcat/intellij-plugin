package com.configcat.intellij.plugin.services

import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.EnvironmentsApi
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.EnvironmentModel
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import io.mockk.*
import java.util.UUID

class FlagViewOpenHandlerTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockEnvironmentsApi: EnvironmentsApi
    private lateinit var mockToolWindow: ToolWindow
    private lateinit var mockContentManager: ContentManager
    private val productId = UUID.randomUUID()
    private val configId = UUID.randomUUID()
    private val settingId = 42

    override fun setUp() {
        super.setUp()

        mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.configcat.com"

        mockEnvironmentsApi = mockk(relaxed = true)
        mockkObject(ConfigCatService)
        every { ConfigCatService.createEnvironmentsService(any(), any()) } returns mockEnvironmentsApi

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) } just Runs

        mockContentManager = mockk(relaxed = true)
        mockToolWindow = mockk(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) } returns mockToolWindow
        project.replaceService(ToolWindowManager::class.java, mockToolWindowManager, testRootDisposable)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // Successfully created view
    // -------------------------------------------------------------------------

    fun testOpenFlagView_success_createsContentAndReturnsTrue() {
        val envModel = createEnvironmentModel("Production", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns true
        every { anyConstructed<EnvironmentSelectDialog>().selectedEnvironment } returns
                EnvironmentSelectDialog.EnvironmentDropDown("Production", envModel.environmentId.toString())

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "MyFlag")

        assertTrue("openFlagView should return true on success", result)
        verify { mockContentManager.addContent(any()) }
        verify { mockContentManager.setSelectedContent(any()) }
    }

    // -------------------------------------------------------------------------
    // ToolWindow not found (null project scenario)
    // -------------------------------------------------------------------------

    fun testOpenFlagView_nullProject_returnsFalse() {
        val envModel = createEnvironmentModel("Production", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns true
        every { anyConstructed<EnvironmentSelectDialog>().selectedEnvironment } returns
                EnvironmentSelectDialog.EnvironmentDropDown("Production", envModel.environmentId.toString())

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(null, mockState, configModel, settingId, "MyFlag")

        assertFalse("openFlagView should return false when project is null", result)
        verify(exactly = 0) { mockContentManager.addContent(any()) }
    }

    fun testOpenFlagView_toolWindowNotFound_returnsFalse() {
        val envModel = createEnvironmentModel("Production", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns true
        every { anyConstructed<EnvironmentSelectDialog>().selectedEnvironment } returns
                EnvironmentSelectDialog.EnvironmentDropDown("Production", envModel.environmentId.toString())

        // Make ToolWindowManager return null for the tool window
        val mockToolWindowManager = mockk<ToolWindowManager>(relaxed = true)
        every { mockToolWindowManager.getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID) } returns null
        project.replaceService(ToolWindowManager::class.java, mockToolWindowManager, testRootDisposable)

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "MyFlag")

        assertFalse("openFlagView should return false when tool window is not found", result)
        verify(exactly = 0) { mockContentManager.addContent(any()) }
    }

    // -------------------------------------------------------------------------
    // No selected environment (dialog cancelled)
    // -------------------------------------------------------------------------

    fun testOpenFlagView_dialogCancelled_returnsFalse() {
        val envModel = createEnvironmentModel("Production", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns false

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "MyFlag")

        assertFalse("openFlagView should return false when dialog is cancelled", result)
        verify(exactly = 0) { mockContentManager.addContent(any()) }
    }

    // -------------------------------------------------------------------------
    // API exception when fetching environments
    // -------------------------------------------------------------------------

    fun testOpenFlagView_apiException_returnsFalse() {
        every { mockEnvironmentsApi.getEnvironments(productId) } throws ApiException(500, "Server Error")

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "MyFlag")

        assertFalse("openFlagView should return false when api throws", result)
        verify {
            ErrorHandler.errorNotify(
                any<ApiException>(),
                "Failed to get environment list. For more information check the logs.",
                any()
            )
        }
    }

    // -------------------------------------------------------------------------
    // Selected environment is null (selectedEnvironment?.id returns "")
    // -------------------------------------------------------------------------

    fun testOpenFlagView_nullSelectedEnvironment_usesEmptyEnvironmentId() {
        val envModel = createEnvironmentModel("Production", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns true
        every { anyConstructed<EnvironmentSelectDialog>().selectedEnvironment } returns null

        val configModel = createConfigModel()

        val result = FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "MyFlag")

        assertFalse("openFlagView should return false with null selectedEnvironment", result)
        verify(exactly = 0) { mockContentManager.addContent(any()) }
    }

    // -------------------------------------------------------------------------
    // Content tab name includes setting name and environment name
    // -------------------------------------------------------------------------

    fun testOpenFlagView_success_contentTabHasCorrectName() {
        val envModel = createEnvironmentModel("Staging", UUID.randomUUID())
        every { mockEnvironmentsApi.getEnvironments(productId) } returns listOf(envModel)

        mockkConstructor(EnvironmentSelectDialog::class)
        every { anyConstructed<EnvironmentSelectDialog>().showAndGet() } returns true
        every { anyConstructed<EnvironmentSelectDialog>().selectedEnvironment } returns
                EnvironmentSelectDialog.EnvironmentDropDown("Staging", envModel.environmentId.toString())

        val addedContents = mutableListOf<Content>()
        every { mockContentManager.addContent(capture(addedContents)) } just Runs

        val configModel = createConfigModel()

        FlagViewOpenHandler.openFlagView(project, mockState, configModel, settingId, "TestFlag")

        assertEquals(1, addedContents.size)
        assertEquals("TestFlag (Staging)", addedContents[0].displayName)
        assertTrue("Content should be closeable", addedContents[0].isCloseable)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createConfigModel(): ConfigModel {
        val productModel = mockk<ProductModel>(relaxed = true)
        every { productModel.productId } returns productId

        val configModel = mockk<ConfigModel>(relaxed = true)
        every { configModel.product } returns productModel
        every { configModel.configId } returns configId
        every { configModel.evaluationVersion } returns EvaluationVersion.V2

        return configModel
    }

    private fun createEnvironmentModel(name: String, id: UUID): EnvironmentModel {
        val envModel = mockk<EnvironmentModel>(relaxed = true)
        every { envModel.name } returns name
        every { envModel.environmentId } returns id
        return envModel
    }
}

