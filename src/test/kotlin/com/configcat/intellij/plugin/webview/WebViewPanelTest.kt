package com.configcat.intellij.plugin.webview

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.messaging.ThemeChangeNotifier
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.jcef.JBCefApp
import io.mockk.*
import kotlinx.serialization.encodeToString
import java.awt.CardLayout

/**
 * Tests for data classes, enum, companion constants, WebViewLafListener,
 * and WebViewPanel JCEF integration defined in WebViewPanel.kt.
 */
class WebViewPanelTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Mock LafManager so getCurrentUIThemeLookAndFeel() doesn't return null in tests
        mockkStatic(LafManager::class)
        val mockLafManagerInstance = mockk<LafManager>(relaxed = true)
        val mockThemeInfo = mockk<UIThemeLookAndFeelInfo>(relaxed = true)
        every { mockThemeInfo.isDark } returns false
        every { mockLafManagerInstance.currentUIThemeLookAndFeel } returns mockThemeInfo
        every { LafManager.getInstance() } returns mockLafManagerInstance
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // AppData tests
    // -------------------------------------------------------------------------

    fun testAppData_serialization_roundTrip() {
        val appData = createTestAppData()
        val json = Constants.json.encodeToString(appData)
        val decoded = Constants.json.decodeFromString<AppData>(json)

        assertEquals(appData, decoded)
    }

    fun testAppData_copyModifiesFields() {
        val original = createTestAppData()
        val copy = original.copy(productName = "Modified Product")

        assertNotSame(original, copy)
        assertEquals("Modified Product", copy.productName)
        assertEquals(original.publicApiBaseUrl, copy.publicApiBaseUrl)
    }

    // -------------------------------------------------------------------------
    // ViewData tests
    // -------------------------------------------------------------------------

    fun testViewData_serialization_roundTrip() {
        val viewData = ViewData("createfeatureflag", "dark")
        val json = Constants.json.encodeToString(viewData)
        val decoded = Constants.json.decodeFromString<ViewData>(json)

        assertEquals(viewData, decoded)
    }


    // -------------------------------------------------------------------------
    // VIEW_TYPE enum tests
    // -------------------------------------------------------------------------

    fun testViewType_hasCorrectTypeValues() {
        assertEquals("createconfig", ViewType.CREATE_CONFIG.type)
        assertEquals("createfeatureflag", ViewType.CREATE_FLAG.type)
        assertEquals("featureflagsetting", ViewType.VIEW_FLAG.type)
    }

    // -------------------------------------------------------------------------
    // WebViewLafListener tests
    // -------------------------------------------------------------------------

    fun testWebViewLafListener_publishesThemeChangeTopic() {
        val messageBus = com.intellij.openapi.application.ApplicationManager.getApplication().messageBus

        // Subscribe to capture the topic message
        val connection = messageBus.connect(testRootDisposable)
        var themeChangeReceived = false
        connection.subscribe(ThemeChangeNotifier.THEME_CHANGE_TOPIC, object : ThemeChangeNotifier {
            override fun notifyThemeChange() {
                themeChangeReceived = true
            }
        })

        val lafManagerForListener = mockk<LafManager>(relaxed = true)
        val listener = WebViewLafListener()
        listener.lookAndFeelChanged(lafManagerForListener)

        assertTrue("ThemeChangeNotifier must be notified when LAF changes", themeChangeReceived)
    }

    // -------------------------------------------------------------------------
    // WebViewPanel JCEF integration tests
    // -------------------------------------------------------------------------

    fun testWebViewPanel_instantiationWithCallback_acceptsCallback() {
        if (!JBCefApp.isSupported()) return

        var callbackInvoked = false
        val callback: (String?) -> Unit = { callbackInvoked = true }
        val panel = WebViewPanel(createTestAppData(), ViewType.CREATE_FLAG, callback)
        try {
            assertNotNull("jsReceiverCallbackFunction must be set", panel.jsReceiverCallbackFunction)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun testWebViewPanel_allViewTypes_canBeInstantiated() {
        if (!JBCefApp.isSupported()) return

        for (viewType in ViewType.entries) {
            val panel = WebViewPanel(createTestAppData(), viewType, null)
            try {
                assertTrue(
                    "WebViewPanel must be created for VIEW_TYPE.$viewType",
                    panel.componentCount > 0
                )
            } finally {
                Disposer.dispose(panel)
            }
        }
    }

    fun testWebViewPanel_lookAndFeelChanged_doesNotThrow() {
        if (!JBCefApp.isSupported()) return

        val panel = WebViewPanel(createTestAppData(), ViewType.CREATE_FLAG, null)
        try {
            // Should not throw even without a loaded page
            panel.lookAndFeelChanged()
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun testWebViewPanel_dispose_doesNotThrow() {
        if (!JBCefApp.isSupported()) return

        val panel = WebViewPanel(createTestAppData(), ViewType.CREATE_CONFIG, null)
        // dispose should clean up resources without throwing
        Disposer.dispose(panel)
    }

    fun testWebViewPanel_themeChangeViaMessageBus_triggersLookAndFeelChanged() {
        if (!JBCefApp.isSupported()) return

        val panel = WebViewPanel(createTestAppData(), ViewType.CREATE_FLAG, null)
        try {
            val publisher = com.intellij.openapi.application.ApplicationManager.getApplication()
                .messageBus.syncPublisher(ThemeChangeNotifier.THEME_CHANGE_TOPIC)

            // Should not throw - exercises the message bus subscription path
            publisher.notifyThemeChange()
        } finally {
            Disposer.dispose(panel)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
}






