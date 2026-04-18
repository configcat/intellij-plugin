package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.jcef.JBCefApp
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.Component
import java.awt.Container
import javax.swing.JEditorPane
import javax.swing.JLabel

class ViewFlagPanelTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()
        mockkStatic(JBCefApp::class)
        every { JBCefApp.isSupported() } returns false
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

