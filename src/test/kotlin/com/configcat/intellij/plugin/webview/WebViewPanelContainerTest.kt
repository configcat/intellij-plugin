package com.configcat.intellij.plugin.webview

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.jcef.JBCefApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import javax.swing.JEditorPane
import javax.swing.JLabel

class WebViewPanelContainerTest : LightPlatformTestCase() {

    private lateinit var testAppData: AppData

    override fun setUp() {
        super.setUp()
        testAppData = createTestAppData()
        mockkStatic(JBCefApp::class)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // JCEF not supported – layout & structure
    // -------------------------------------------------------------------------

    fun testJcefNotSupported_layoutAndStructure() {
        every { JBCefApp.isSupported() } returns false

        val container = buildContainer()

        // container must use a card layout
        assertTrue(
            "Container must use CardLayout when JCEF is not supported",
            container.layout is CardLayout
        )
        // container has exactly one child panel (the error message)
        assertEquals(
            "Exactly one child panel must be added when JCEF is not supported",
            1,
            container.componentCount
        )
        // no web view panel is present in the component tree
        assertFalse(
            "WebViewPanel must NOT be present in the component tree when JCEF is not supported",
            allComponents(container).any { it is WebViewPanel }
        )
    }

    // -------------------------------------------------------------------------
    // JCEF not supported – error message content
    // -------------------------------------------------------------------------

    fun testJcefNotSupported_mainErrorLabelIsShown() {
        every { JBCefApp.isSupported() } returns false

        val container = buildContainer()

        assertTrue(
            "Primary error label 'JCEF (Java Chromium Embedded Framework) is not supported.' must be present",
            allText(container).any {
                it.contains("JCEF (Java Chromium Embedded Framework) is not supported.")
            }
        )
    }


    // -------------------------------------------------------------------------
    // JCEF not supported – parameterised over VIEW_TYPE
    // -------------------------------------------------------------------------

    fun testJcefNotSupported_errorPanelShownForAllViewTypes() {
        every { JBCefApp.isSupported() } returns false

        for (viewType in VIEW_TYPE.entries) {
            val container = buildContainer(viewType = viewType)

            assertEquals(
                "Error panel must be shown for VIEW_TYPE.$viewType",
                1,
                container.componentCount
            )
            assertTrue(
                "Error label must be present for VIEW_TYPE.$viewType",
                allText(container).any {
                    it.contains("JCEF (Java Chromium Embedded Framework) is not supported.")
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildContainer(
        viewType: VIEW_TYPE = VIEW_TYPE.CREATE_FLAG,
        callback: ((String?) -> Unit)? = null,
    ): WebViewPanelContainer =
        WebViewPanelContainer(
            project = mockk(relaxed = true),
            appData = testAppData,
            viewType = viewType,
            jsReceiverCallbackFunction = callback,
        )

    private fun createTestAppData() = AppData(
        publicApiBaseUrl = "https://api.configcat.com",
        basicAuthUsername = "testUser",
        basicAuthPassword = "testPass",
        dashboardBasePath = "https://app.configcat.com",
        productId = "test-product-id",
        productName = "Test Product",
        configId = "test-config-id",
        configName = "Test Config",
        environmentId = "test-env-id",
        evaluationVersion = "V1",
        settingId = "test-setting-id",
    )

    /**
     * Recursively collects every visible text string from [container]'s
     * component tree.  Both plain [JLabel] and HTML-based [JEditorPane]
     * components are included so that IntelliJ's UI-DSL `label {}` and
     * `text {}` cells are both covered.
     */
    private fun allText(container: Container): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until container.componentCount) {
            val c: Component = container.getComponent(i)
            when (c) {
                is JLabel -> c.text?.takeIf { it.isNotEmpty() }?.let { result += it }
                is JEditorPane -> c.text?.takeIf { it.isNotEmpty() }?.let { result += it }
            }
            if (c is Container) result += allText(c)
        }
        return result
    }

    /** Recursively collects every [Component] in the tree rooted at [container]. */
    private fun allComponents(container: Container): List<Component> {
        val result = mutableListOf<Component>()
        for (i in 0 until container.componentCount) {
            val c: Component = container.getComponent(i)
            result += c
            if (c is Container) result += allComponents(c)
        }
        return result
    }
}


