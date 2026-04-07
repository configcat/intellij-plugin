package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingType
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

class CreateFlagDialogTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockNodeDataService: ConfigCatNodeDataService

    private val productId: UUID = UUID.randomUUID()
    private val configId: UUID = UUID.randomUUID()

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
    // SettingTypeDropDown – toString
    // -------------------------------------------------------------------------

    fun testSettingTypeDropDown_toString_returnsName() {
        val dropDown = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)

        assertEquals("Boolean", dropDown.toString())
    }

    fun testSettingTypeDropDown_toString_returnsEmptyWhenNameIsEmpty() {
        val dropDown = CreateFlagDialog.SettingTypeDropDown("", SettingType.STRING)

        assertEquals("", dropDown.toString())
    }

    // -------------------------------------------------------------------------
    // SettingTypeDropDown – compareTo
    // -------------------------------------------------------------------------

    fun testSettingTypeDropDown_compareTo_equalWhenSameType() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("Bool", SettingType.BOOLEAN)

        assertEquals("compareTo must return 0 for equal SettingType values", 0, first.compareTo(second))
    }

    fun testSettingTypeDropDown_compareTo_orderedBySettingType() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("String", SettingType.STRING)

        val result = first.compareTo(second)
        // Just verify the ordering is consistent (not zero), direction depends on enum ordinal
        assertEquals(
            "compareTo must be consistent with SettingType.compareTo",
            SettingType.BOOLEAN.compareTo(SettingType.STRING),
            result
        )
    }

    // -------------------------------------------------------------------------
    // SettingTypeDropDown – data class equality & copy
    // -------------------------------------------------------------------------

    fun testSettingTypeDropDown_equality_sameValues_areEqual() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)

        assertEquals(first, second)
    }

    fun testSettingTypeDropDown_equality_differentType_areNotEqual() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.STRING)

        assertFalse("Instances with different SettingType must not be equal", first == second)
    }

    fun testSettingTypeDropDown_equality_differentName_areNotEqual() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("Bool", SettingType.BOOLEAN)

        assertFalse("Instances with different names must not be equal", first == second)
    }

    fun testSettingTypeDropDown_copy_preservesTypeAndChangesName() {
        val original = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val copy = original.copy(name = "Bool Flag")

        assertEquals(SettingType.BOOLEAN, copy.type)
        assertEquals("Bool Flag", copy.name)
        assertNotSame(original, copy)
    }

    fun testSettingTypeDropDown_hashCode_equalObjectsHaveSameHashCode() {
        val first = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)
        val second = CreateFlagDialog.SettingTypeDropDown("Boolean", SettingType.BOOLEAN)

        assertEquals(first.hashCode(), second.hashCode())
    }

    // -------------------------------------------------------------------------
    // Dialog initialization
    // -------------------------------------------------------------------------

    fun testDialog_title_isCreateFlag() {
        val dialog = buildDialog()
        try {
            assertEquals("Create Flag", dialog.title)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testDialog_createActions_returnsEmptyArray() {
        val dialog = buildDialog()
        try {
            val method = CreateFlagDialog::class.java.getDeclaredMethod("createActions")
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
            dialog.saveSuccess("new-flag-id")

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Feature Flag Successfully created.") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_callsLoadFlagsWithConfigId() {
        val dialog = buildDialog()
        try {
            dialog.saveSuccess("new-flag-id")

            verify(exactly = 1) { mockNodeDataService.loadFlags(configId) }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_nullReturnId_stillNotifiesAndLoadsFlags() {
        val dialog = buildDialog()
        try {
            dialog.saveSuccess(null)

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Feature Flag Successfully created.") }
            verify(exactly = 1) { mockNodeDataService.loadFlags(configId) }
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // saveSuccess – error path
    // -------------------------------------------------------------------------

    fun testSaveSuccess_whenLoadFlagsThrowsApiException_notifiesError() {
        every { mockNodeDataService.loadFlags(configId) } throws ApiException(500, "Server Error")

        val dialog = buildDialog()
        try {
            dialog.saveSuccess("some-flag-id")

            verify(exactly = 1) { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testSaveSuccess_whenLoadFlagsThrowsApiException_stillNotifiesInfoFirst() {
        every { mockNodeDataService.loadFlags(configId) } throws ApiException(401, "Unauthorized")

        val dialog = buildDialog()
        try {
            dialog.saveSuccess("some-flag-id")

            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Feature Flag Successfully created.") }
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildDialog(): CreateFlagDialog {
        val product = mockk<ProductModel>(relaxed = true)
        every { product.productId } returns productId
        every { product.name } returns "Test Product"

        val config = mockk<ConfigModel>(relaxed = true)
        every { config.configId } returns configId
        every { config.name } returns "Test Config"
        every { config.product } returns product

        return CreateFlagDialog(project = null, config = config)
    }

    private fun safeDispose(dialog: CreateFlagDialog) {
        try {
            Disposer.dispose(dialog.disposable)
        } catch (_: Exception) {
        }
    }
}
