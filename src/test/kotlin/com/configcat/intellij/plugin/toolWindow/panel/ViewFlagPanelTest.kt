package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.TestUtils.suppressLogErrors
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.util.Disposer
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
import java.awt.Component
import java.awt.Container
import javax.swing.JEditorPane
import javax.swing.JLabel

class ViewFlagPanelTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()
        mockkStatic(JBCefApp::class)
        every { JBCefApp.isSupported() } returns false

        mockState = mockk(relaxed = true)
        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs
        every { ConfigCatNotifier.Notify.info(any()) } just Runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testInit_addsWebViewPanelContainerWithExpectedAlignment() {
        val panel = buildPanel()

        assertEquals("ViewFlagPanel must set LEFT alignment", Component.LEFT_ALIGNMENT, panel.alignmentX)
        assertEquals("ViewFlagPanel must set TOP alignment", Component.TOP_ALIGNMENT, panel.alignmentY)
        assertEquals("ViewFlagPanel must add exactly one child component", 1, panel.componentCount)
        assertTrue(
            "Child component must be WebViewPanelContainer",
            panel.getComponent(0) is WebViewPanelContainer
        )
    }

    fun testInit_whenJcefNotSupported_showsFallbackMessageInsideContainer() {
        val panel = buildPanel()
        val webViewContainer = panel.getComponent(0) as WebViewPanelContainer
        val allVisibleText = allText(webViewContainer)

        assertTrue(
            "Fallback JCEF unsupported message must be present",
            allVisibleText.any { it.contains("JCEF (Java Chromium Embedded Framework) is not supported.") }
        )
    }

    fun testDispose_doesNotThrow() {
        val panel = buildPanel()

        panel.dispose()
    }

    // -------------------------------------------------------------------------
    // processViewFlagResponse tests
    // -------------------------------------------------------------------------

    fun testProcessViewFlagResponse_invalidJson_notifiesError() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse("not valid json")
        }

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: invalid response from the server.") }
    }

    fun testProcessViewFlagResponse_null_notifiesError() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse(null)
        }

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: invalid response from the server.") }
    }

    fun testProcessViewFlagResponse_emptyString_notifiesError() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse("")
        }

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: invalid response from the server.") }
    }

    fun testProcessViewFlagResponse_webviewFail_notifiesErrorWithMessage() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse(
                """{"type":"webview-fail","data":{"message":"Component loading failed","status":500}}"""
            )
        }

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: Component loading failed") }
    }

    fun testProcessViewFlagResponse_noneType_notifiesInvalidResponse() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse(
                """{"type":"none","data":null}"""
            )
        }

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: invalid response from the server.") }
    }

    fun testProcessViewFlagResponse_webviewFail401_callsUnAuthenticate() {
        val panel = buildPanel()

        suppressLogErrors {
            panel.processViewFlagResponse(
                """{"type":"webview-fail","data":{"message":"Unauthorized","status":401}}"""
            )
        }

        verify(exactly = 1) { mockState.unAuthenticate() }
        verify(exactly = 1) { ConfigCatNotifier.Notify.error("Logged out from ConfigCat. Please re-authenticate to continue.") }
        verify(exactly = 1) { ConfigCatNotifier.Notify.error("View Flag failed: Unauthorized") }
    }

    private fun buildPanel(): ViewFlagPanel {
        val panel = ViewFlagPanel(createTestAppData())
        Disposer.register(testRootDisposable, panel)
        return panel
    }

    private fun createTestAppData() = AppData(
        publicApiBaseUrl = "https://api.configcat.com",
        basicAuthUsername = "test-user",
        basicAuthPassword = "test-pass",
        dashboardBasePath = "https://app.configcat.com",
        isAuthorized = true,
        productId = "product-id",
        productName = "Product",
        configId = "config-id",
        configName = "Config",
        environmentId = "environment-id",
        evaluationVersion = "V1",
        settingId = "setting-id",
    )

    private fun allText(container: Container): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            when (component) {
                is JLabel -> component.text?.takeIf { it.isNotEmpty() }?.let { result += it }
                is JEditorPane -> component.text?.takeIf { it.isNotEmpty() }?.let { result += it }
            }
            if (component is Container) {
                result += allText(component)
            }
        }
        return result
    }
}

