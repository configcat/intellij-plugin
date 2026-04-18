package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.HelpPanel
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.panel.ViewFlagPanel
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.api.ConfigsApi
import com.configcat.publicapi.java.client.api.FeatureFlagsSettingsApi
import com.configcat.publicapi.java.client.api.ProductsApi
import com.intellij.openapi.wm.ToolWindow
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.content.ContentManager
import com.intellij.ui.jcef.JBCefApp
import io.mockk.*
import java.awt.BorderLayout

class ConfigCatToolWindowFactoryTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        every { mockState.isConfigured() } returns false

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns mockk(relaxed = true)

        mockkObject(ConfigCatPropertiesService.Companion)
        every { ConfigCatPropertiesService.getInstance() } returns mockk(relaxed = true)

        mockkObject(ConfigCatService.Companion)
        every { ConfigCatService.createProductsService(any(), any()) } returns mockk<ProductsApi>(relaxed = true)
        every { ConfigCatService.createConfigsService(any(), any()) } returns mockk<ConfigsApi>(relaxed = true)
        every { ConfigCatService.createFeatureFlagsSettingsService(any(), any()) } returns mockk<FeatureFlagsSettingsApi>(relaxed = true)

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any()) } just Runs

        mockkStatic(JBCefApp::class)
        every { JBCefApp.isSupported() } returns false
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // companion object
    // -------------------------------------------------------------------------

    fun testCompanionObject_toolWindowIdConstant() {
        assertEquals("ConfigCat", ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
    }

    // -------------------------------------------------------------------------
    // shouldBeAvailable
    // -------------------------------------------------------------------------

    fun testShouldBeAvailable_returnsTrue() {
        val factory = ConfigCatToolWindowFactory()
        assertTrue("shouldBeAvailable must always return true", factory.shouldBeAvailable(project))
    }

    // -------------------------------------------------------------------------
    // createToolWindowContent – end-to-end
    // -------------------------------------------------------------------------

    fun testCreateToolWindowContent_addsTwoTabs() {
        val factory = ConfigCatToolWindowFactory()

        val mockContentManager = mockk<ContentManager>(relaxed = true)
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        factory.createToolWindowContent(project, mockToolWindow)

        verify(exactly = 2) { mockContentManager.addContent(any()) }
    }

    fun testCreateToolWindowContent_firstTabIsProductsAndConfigs() {
        val factory = ConfigCatToolWindowFactory()

        val addedContents = mutableListOf<com.intellij.ui.content.Content>()
        val mockContentManager = mockk<ContentManager>(relaxed = true)
        every { mockContentManager.addContent(capture(addedContents)) } just Runs
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        factory.createToolWindowContent(project, mockToolWindow)

        assertEquals(2, addedContents.size)
        assertEquals("Products & Configs", addedContents[0].displayName)
        assertFalse("Products & Configs tab must not be closeable", addedContents[0].isCloseable)
    }

    fun testCreateToolWindowContent_secondTabIsFeatureFlagsAndSettings() {
        val factory = ConfigCatToolWindowFactory()

        val addedContents = mutableListOf<com.intellij.ui.content.Content>()
        val mockContentManager = mockk<ContentManager>(relaxed = true)
        every { mockContentManager.addContent(capture(addedContents)) } just Runs
        val mockToolWindow = mockk<ToolWindow>(relaxed = true)
        every { mockToolWindow.contentManager } returns mockContentManager

        factory.createToolWindowContent(project, mockToolWindow)

        assertEquals(2, addedContents.size)
        assertEquals("Feature Flags & Settings", addedContents[1].displayName)
        assertFalse("Feature Flags & Settings tab must not be closeable", addedContents[1].isCloseable)
    }

    // -------------------------------------------------------------------------
    // ProductsConfigsTreeToolWindow.getContent()
    // -------------------------------------------------------------------------

    fun testProductsConfigsTreeToolWindow_getContent_layoutAndTooltip() {
        val toolWindow = ConfigCatToolWindowFactory.ProductsConfigsTreeToolWindow(project)
        val content = toolWindow.getContent()

        assertTrue("Content must use BorderLayout", content.layout is BorderLayout)
        assertEquals("Manage products abd configs.", content.toolTipText)
        assertEquals("Content must have exactly one child", 1, content.componentCount)
        assertTrue(
            "Child must be a ProductsConfigsPanel",
            content.getComponent(0) is ProductsConfigsPanel
        )
    }

    // -------------------------------------------------------------------------
    // FeatureFlagsTreeToolWindow.getContent()
    // -------------------------------------------------------------------------

    fun testFeatureFlagsTreeToolWindow_getContent_layoutAndTooltip() {
        val toolWindow = ConfigCatToolWindowFactory.FeatureFlagsTreeToolWindow(project)
        val content = toolWindow.getContent()

        assertTrue("Content must use BorderLayout", content.layout is BorderLayout)
        assertEquals("Manage connected config's flags and settings.", content.toolTipText)
        assertEquals("Content must have exactly one child", 1, content.componentCount)
        assertTrue(
            "Child must be a SettingsPanel",
            content.getComponent(0) is SettingsPanel
        )
    }

    // -------------------------------------------------------------------------
    // HelpToolWindow.getContent()
    // -------------------------------------------------------------------------

    fun testHelpToolWindow_getContent_layoutAndTooltip() {
        val toolWindow = ConfigCatToolWindowFactory.HelpToolWindow()
        val content = toolWindow.getContent()

        assertTrue("Content must use BorderLayout", content.layout is BorderLayout)
        assertEquals("Useful links.", content.toolTipText)
        assertEquals("Content must have exactly one child", 1, content.componentCount)
        assertTrue(
            "Child must be a HelpPanel",
            content.getComponent(0) is HelpPanel
        )
    }

    // -------------------------------------------------------------------------
    // ConfigCatFeatureFlagsViewToolWindow.getContent()
    // -------------------------------------------------------------------------

    fun testFeatureFlagsViewToolWindow_getContent_layoutAndChild() {
        val toolWindow = ConfigCatToolWindowFactory.ConfigCatFeatureFlagsViewToolWindow(createTestAppData())
        val content = toolWindow.getContent()

        assertTrue("Content must use BorderLayout", content.layout is BorderLayout)
        assertEquals("Content must have exactly one child", 1, content.componentCount)
        assertTrue(
            "Child must be a ViewFlagPanel",
            content.getComponent(0) is ViewFlagPanel
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
}

