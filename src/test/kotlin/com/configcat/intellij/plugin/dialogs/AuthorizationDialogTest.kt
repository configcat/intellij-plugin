package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.TestUtils.safeDispose
import com.configcat.intellij.plugin.TestUtils.suppressLogErrors
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.jcef.JBCefApp
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify

class AuthorizationDialogTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"stored-user","basicAuthPassword":"stored-pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.configcat.com"
        every { mockState.dashboardBaseUrl } returns "https://app.configcat.com"

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.info(any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockkStatic(JBCefApp::class)
        every { JBCefApp.isSupported() } returns false
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testDialog_title_isAuthorization() {
        val dialog = buildDialog()
        try {
            assertEquals("Authorization", dialog.title)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testDialog_createActions_returnsEmptyArray() {
        val dialog = buildDialog()
        try {
            val method = AuthorizationDialog::class.java.getDeclaredMethod("createActions")
            method.isAccessible = true
            val actions = method.invoke(dialog) as Array<*>

            assertEquals("createActions must return an empty array (OK and Cancel removed)", 0, actions.size)
        } finally {
            safeDispose(dialog)
        }
    }

    fun testProcessAuthorizationResponse_json_setsAuthorizationModel_andNotifiesLogin() {
        val dialog = buildDialog()
        try {
            dialog.processAuthorizationResponse(
                """{"type":"authorization","data":{"basicAuthUsername":"demo-user","basicAuthPassword":"demo-pass","email":"demo@example.com","fullName":"Demo User"}}"""
            )

            assertEquals(
                AuthorizationDialog.AuthorizationModel(
                    basicAuthUsername = "demo-user",
                    basicAuthPassword = "demo-pass",
                    email = "demo@example.com",
                    fullName = "Demo User",
                ),
                dialog.authorizationModel,
            )
            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Logged in to ConfigCat. Email: demo@example.com") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testProcessAuthorizationResponse_unauthorize_clearsAuthorizationModel_andNotifiesLogout() {
        val dialog = buildDialog()
        try {
            setAuthorizationModel(
                dialog,
                AuthorizationDialog.AuthorizationModel(
                    basicAuthUsername = "demo-user",
                    basicAuthPassword = "demo-pass",
                    email = "demo@example.com",
                    fullName = "Demo User",
                ),
            )

            dialog.processAuthorizationResponse("""{"type":"authorization","data":"unauthorize"}""")

            assertNull(dialog.authorizationModel)
            verify(exactly = 1) { ConfigCatNotifier.Notify.info("Logged out from ConfigCat.") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testProcessAuthorizationResponse_null_treatsAsError_keepsAuthorizationModel() {
        val dialog = buildDialog()
        try {
            val existingModel = AuthorizationDialog.AuthorizationModel(
                basicAuthUsername = "demo-user",
                basicAuthPassword = "demo-pass",
                email = "demo@example.com",
                fullName = "Demo User",
            )
            setAuthorizationModel(dialog, existingModel)

            suppressLogErrors {
                dialog.processAuthorizationResponse(null)
            }

            assertEquals(existingModel, dialog.authorizationModel)
            verify(exactly = 1) { ConfigCatNotifier.Notify.error("Authorization failed: invalid response from the server.") }
        } finally {
            safeDispose(dialog)
        }
    }

    private fun buildDialog(): AuthorizationDialog = AuthorizationDialog()

    private fun setAuthorizationModel(
        dialog: AuthorizationDialog,
        authorizationModel: AuthorizationDialog.AuthorizationModel?,
    ) {
        val field = AuthorizationDialog::class.java.getDeclaredField("authorizationModel")
        field.isAccessible = true
        field.set(dialog, authorizationModel)
    }

    // -------------------------------------------------------------------------
    // processResponseData – webview-fail type
    // -------------------------------------------------------------------------

    fun testProcessAuthorizationResponse_webviewFail_notifiesError_andKeepsModel() {
        val dialog = buildDialog()
        try {
            val existingModel = AuthorizationDialog.AuthorizationModel(
                basicAuthUsername = "demo-user",
                basicAuthPassword = "demo-pass",
                email = "demo@example.com",
                fullName = "Demo User",
            )
            setAuthorizationModel(dialog, existingModel)

            suppressLogErrors {
                dialog.processAuthorizationResponse(
                    """{"type":"webview-fail","data":{"message":"Something went wrong","status":500}}"""
                )
            }

            assertEquals(existingModel, dialog.authorizationModel)
            verify(exactly = 1) { ConfigCatNotifier.Notify.error("Authorization failed: Something went wrong") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testProcessAuthorizationResponse_webviewFail401_callsUnAuthenticate() {
        val dialog = buildDialog()
        try {
            val existingModel = AuthorizationDialog.AuthorizationModel(
                basicAuthUsername = "demo-user",
                basicAuthPassword = "demo-pass",
                email = "demo@example.com",
                fullName = "Demo User",
            )
            setAuthorizationModel(dialog, existingModel)

            suppressLogErrors {
                dialog.processAuthorizationResponse(
                    """{"type":"webview-fail","data":{"message":"Unauthorized","status":401}}"""
                )
            }

            assertEquals(existingModel, dialog.authorizationModel)
            verify(exactly = 1) { mockState.unAuthenticate() }
            verify(exactly = 1) { ConfigCatNotifier.Notify.error("Authorization failed: Unauthorized") }
        } finally {
            safeDispose(dialog)
        }
    }

    fun testProcessAuthorizationResponse_noneType_treatsAsUnexpected() {
        val dialog = buildDialog()
        try {
            val existingModel = AuthorizationDialog.AuthorizationModel(
                basicAuthUsername = "demo-user",
                basicAuthPassword = "demo-pass",
                email = "demo@example.com",
                fullName = "Demo User",
            )
            setAuthorizationModel(dialog, existingModel)

            suppressLogErrors {
                dialog.processAuthorizationResponse(
                    """{"type":"none","data":null}"""
                )
            }

            assertEquals(existingModel, dialog.authorizationModel)
            verify(exactly = 1) { ConfigCatNotifier.Notify.error("Authorization failed: invalid response from the server.") }
        } finally {
            safeDispose(dialog)
        }
    }

}
