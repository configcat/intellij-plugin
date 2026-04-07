package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.jcef.JBCefApp
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID

class CreateConfigDialogTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockNodeDataService: ConfigCatNodeDataService

    private val productId: UUID = UUID.randomUUID()

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.configcat.com"
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        mockNodeDataService = mockk(relaxed = true)
        every { ConfigCatNodeDataService.getInstance() } returns mockNodeDataService

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.info(any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) } just Runs

        mockkStatic(JBCefApp::class)
        every { JBCefApp.isSupported() } returns false
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // Dialog initialization
    // -------------------------------------------------------------------------

    fun testDialog_title_isCreateConfig() {
        val dialog = buildDialog()
        try {
            assertEquals("Create Config", dialog.title)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testDialog_createActions_returnsEmptyArray() {
        val dialog = buildDialog()
        try {
            val method = CreateConfigDialog::class.java.getDeclaredMethod("createActions")
            method.isAccessible = true
            val actions = method.invoke(dialog) as Array<*>

            assertEquals("createActions must return an empty array (OK and Cancel removed)", 0, actions.size)
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // saveSuccess – happy path
    // -------------------------------------------------------------------------

    fun testSaveSuccess_notifiesInfoMessage() {
        val dialog = buildDialog()
        try {
            dialog.saveSuccess("new-config-id")

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Config Successfully created.") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_callsLoadConfigsWithProductId() {
        val dialog = buildDialog()
        try {
            dialog.saveSuccess("new-config-id")

            verify(exactly = 1) { mockNodeDataService.loadConfigs(productId) }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_nullReturnId_stillNotifiesAndLoadsConfigs() {
        val dialog = buildDialog()
        try {
            dialog.saveSuccess(null)

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Config Successfully created.") }
            verify(exactly = 1) { mockNodeDataService.loadConfigs(productId) }
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // saveSuccess – error path
    // -------------------------------------------------------------------------

    fun testSaveSuccess_whenLoadConfigsThrowsApiException_notifiesError() {
        every { mockNodeDataService.loadConfigs(productId) } throws ApiException(500, "Server Error")

        val dialog = buildDialog()
        try {
            dialog.saveSuccess("some-id")

            verify(exactly = 1) { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_whenLoadConfigsThrowsApiException_stilNotifiesInfoFirst() {
        every { mockNodeDataService.loadConfigs(productId) } throws ApiException(401, "Unauthorized")

        val dialog = buildDialog()
        try {
            dialog.saveSuccess("some-id")

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Config Successfully created.") }
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildDialog(): CreateConfigDialog {
        val product = mockk<ProductModel>(relaxed = true)
        every { product.productId } returns productId
        every { product.name } returns "Test Product"
        return CreateConfigDialog(project = null, product = product)
    }

    private fun safeDispose(dialog: CreateConfigDialog) {
        try {
            Disposer.dispose(dialog.disposable)
        } catch (_: Exception) {
        }
    }
}
