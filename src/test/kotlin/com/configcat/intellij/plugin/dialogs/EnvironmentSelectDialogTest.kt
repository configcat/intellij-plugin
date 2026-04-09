package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.model.EnvironmentModel
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.util.UUID

class EnvironmentSelectDialogTest : LightPlatformTestCase() {

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // EnvironmentDropDown – toString
    // -------------------------------------------------------------------------

    fun testEnvironmentDropDown_toString_returnsName() {
        val dropDown = EnvironmentSelectDialog.EnvironmentDropDown("Production", "prod-id")

        assertEquals("Production", dropDown.toString())
    }

    fun testEnvironmentDropDown_toString_returnsEmptyWhenNameIsEmpty() {
        val dropDown = EnvironmentSelectDialog.EnvironmentDropDown("", "some-id")

        assertEquals("", dropDown.toString())
    }

    // -------------------------------------------------------------------------
    // EnvironmentDropDown – compareTo
    // -------------------------------------------------------------------------

    fun testEnvironmentDropDown_compareTo_positiveWhenIdIsGreater() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Z", "z-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("A", "a-id")

        assertTrue("compareTo must return positive when this.id > other.id", first.compareTo(second) > 0)
    }

    fun testEnvironmentDropDown_compareTo_negativeWhenIdIsLess() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("A", "a-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Z", "z-id")

        assertTrue("compareTo must return negative when this.id < other.id", first.compareTo(second) < 0)
    }

    fun testEnvironmentDropDown_compareTo_zeroWhenSameId() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Alpha", "same-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Beta", "same-id")

        assertEquals("compareTo must return 0 when ids are equal", 0, first.compareTo(second))
    }

    // -------------------------------------------------------------------------
    // EnvironmentDropDown – data class equality & copy
    // -------------------------------------------------------------------------

    fun testEnvironmentDropDown_equality_sameValues_areEqual() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Prod", "prod-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Prod", "prod-id")

        assertEquals(first, second)
    }

    fun testEnvironmentDropDown_equality_differentId_areNotEqual() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Env", "id-1")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Env", "id-2")

        assertFalse("Instances with different ids must not be equal", first == second)
    }

    fun testEnvironmentDropDown_equality_differentName_areNotEqual() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Prod", "same-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Dev", "same-id")

        assertFalse("Instances with different names must not be equal", first == second)
    }

    fun testEnvironmentDropDown_copy_preservesIdAndChangesName() {
        val original = EnvironmentSelectDialog.EnvironmentDropDown("Production", "prod-id")
        val copy = original.copy(name = "Staging")

        assertEquals("prod-id", copy.id)
        assertEquals("Staging", copy.name)
        assertNotSame(original, copy)
    }

    fun testEnvironmentDropDown_hashCode_equalObjectsHaveSameHashCode() {
        val first = EnvironmentSelectDialog.EnvironmentDropDown("Prod", "prod-id")
        val second = EnvironmentSelectDialog.EnvironmentDropDown("Prod", "prod-id")

        assertEquals(first.hashCode(), second.hashCode())
    }

    // -------------------------------------------------------------------------
    // Dialog initialization
    // -------------------------------------------------------------------------

    fun testDialog_title_isSelectAnEnvironment() {
        val dialog = buildDialog(emptyList())
        try {
            assertEquals("Select an Environment", dialog.title)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testDialog_okActionName_isSelect() {
        val dialog = buildDialog(emptyList())
        try {
            val getOKAction = com.intellij.openapi.ui.DialogWrapper::class.java
                .getDeclaredMethod("getOKAction")
            getOKAction.isAccessible = true
            val okAction = getOKAction.invoke(dialog) as javax.swing.Action
            assertEquals("Select", okAction.getValue(javax.swing.Action.NAME))
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // environmentsDropDownValidation
    // -------------------------------------------------------------------------

    fun testEnvironmentsDropDownValidation_emptyEnvironmentList_returnsValidationError() {
        val dialog = buildDialog(emptyList())
        try {
            val result = dialog.environmentsDropDownValidation()

            assertNotNull("Validation must fail when the combo box has no selection", result)
            assertTrue(
                "Error message must contain 'Invalid environment'",
                result!!.message.contains("Invalid environment")
            )
        } finally {
            safeDispose(dialog)
        }
    }

    fun testEnvironmentsDropDownValidation_withOneEnvironment_returnsNull() {
        val envModel = mockk<EnvironmentModel>(relaxed = true)
        every { envModel.name } returns "Production"
        every { envModel.environmentId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")

        val dialog = buildDialog(listOf(envModel))
        try {
            val result = dialog.environmentsDropDownValidation()

            assertNull("Validation must pass when an environment is selected", result)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testEnvironmentsDropDownValidation_withMultipleEnvironments_returnsNull() {
        val env1 = mockk<EnvironmentModel>(relaxed = true)
        every { env1.name } returns "Production"
        every { env1.environmentId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")

        val env2 = mockk<EnvironmentModel>(relaxed = true)
        every { env2.name } returns "Staging"
        every { env2.environmentId } returns UUID.fromString("00000000-0000-0000-0000-000000000002")

        val dialog = buildDialog(listOf(env1, env2))
        try {
            val result = dialog.environmentsDropDownValidation()

            assertNull("Validation must pass when environments exist and first is selected", result)
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // doOKAction with null project
    // -------------------------------------------------------------------------

    fun testDoOKAction_nullProject_doesNotThrow() {
        val envModel = mockk<EnvironmentModel>(relaxed = true)
        every { envModel.name } returns "Staging"
        every { envModel.environmentId } returns UUID.fromString("00000000-0000-0000-0000-000000000002")

        val appData = createTestAppData()
        val dialog = buildDialog(listOf(envModel), appData = appData)
        try {
            // project is null, so the project?.let block is skipped; doOKAction must not throw
            val doOkAction = EnvironmentSelectDialog::class.java.getDeclaredMethod("doOKAction")
            doOkAction.isAccessible = true
            doOkAction.invoke(dialog)

            // Verify appData.environmentId was updated with the selected environment's id
            assertEquals("00000000-0000-0000-0000-000000000002", appData.environmentId)
        } finally {
            safeDispose(dialog)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildDialog(
        environments: List<EnvironmentModel>,
        appData: AppData = createTestAppData(),
    ): EnvironmentSelectDialog =
        EnvironmentSelectDialog(
            project = null,
            environments = environments,
            appData = appData,
            settingName = "My Feature Flag",
        )

    private fun createTestAppData() = AppData(
        publicApiBaseUrl = "https://api.configcat.com",
        basicAuthUsername = "user",
        basicAuthPassword = "pass",
        dashboardBasePath = "https://app.configcat.com",
        productId = "test-product",
        productName = "Test Product",
        configId = "test-config",
        configName = "Test Config",
        environmentId = "",
        evaluationVersion = "V2",
        settingId = "",
    )

    private fun safeDispose(dialog: EnvironmentSelectDialog) {
        try {
            Disposer.dispose(dialog.disposable)
        } catch (_: Exception) {
        }
    }
}
