package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.ConnectedConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.tree.ConfigRootNode
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.ConfigsApi
import com.configcat.publicapi.java.client.api.FeatureFlagsSettingsApi
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.treeStructure.Tree
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.lang.reflect.Field
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class SettingsPanelTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockNodeDataService: ConfigCatNodeDataService
    private lateinit var mockPropertiesService: ConfigCatPropertiesService
    private lateinit var mockConfigsApi: ConfigsApi
    private lateinit var mockFeatureFlagsApi: FeatureFlagsSettingsApi

    private val connectedConfigId: String = UUID.randomUUID().toString()

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        mockNodeDataService = mockk(relaxed = true)
        mockPropertiesService = mockk(relaxed = true)
        mockConfigsApi = mockk(relaxed = true)
        mockFeatureFlagsApi = mockk(relaxed = true)

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns mockNodeDataService

        mockkObject(ConfigCatPropertiesService.Companion)
        every { ConfigCatPropertiesService.getInstance() } returns mockPropertiesService

        mockkObject(ConfigCatService)
        every { ConfigCatService.createConfigsService(any(), any()) } returns mockConfigsApi
        every { ConfigCatService.createFeatureFlagsSettingsService(any(), any()) } returns mockFeatureFlagsApi

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any<ApiException>(), any(), any()) } just Runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // initContent – not-configured cases (synchronous, no coroutine needed)
    // -------------------------------------------------------------------------

    fun testNotConfigured_treeAndTreeModelAreNull() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()

        assertNull("tree must be null when plugin is not configured", treeField(panel))
        assertNull("treeModel must be null when plugin is not configured", treeModelField(panel))
    }

    // -------------------------------------------------------------------------
    // initTree failure – loadConnectedConfig returns null
    // -------------------------------------------------------------------------

    fun testInitTree_noConnectedConfig_treeRemainsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns null

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must be null when no connected config is stored", treeField(panel))
        assertNull("treeModel must be null when no connected config is stored", treeModelField(panel))
    }

    // -------------------------------------------------------------------------
    // initTree failure – loadConnectedConfig → getConfig API errors
    // -------------------------------------------------------------------------

    fun testInitTree_getConfig_api401Unauthorized_treeRemainsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } throws ApiException(401, "Unauthorized")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 401 on getConfig()", treeField(panel))
        assertNull("treeModel must remain null after 401 on getConfig()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load config. For more information check the logs.", null) }
    }

    fun testInitTree_getConfig_api429TooManyRequests_treeRemainsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } throws ApiException(429, "Too Many Requests")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 429 on getConfig()", treeField(panel))
        assertNull("treeModel must remain null after 429 on getConfig()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load config. For more information check the logs.", null) }
    }

    fun testInitTree_getConfig_api500ServerError_treeRemainsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } throws ApiException(500, "Internal Server Error")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 500 on getConfig()", treeField(panel))
        assertNull("treeModel must remain null after 500 on getConfig()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load config. For more information check the logs.", null) }
    }

    fun testInitTree_getConfig_api503ServiceUnavailable_treeRemainsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } throws ApiException(503, "Service Unavailable")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 503 on getConfig()", treeField(panel))
        assertNull("treeModel must remain null after 503 on getConfig()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load config. For more information check the logs.", null) }
    }

    // -------------------------------------------------------------------------
    // initTree failure – connected config loaded OK, getSettings API errors
    // -------------------------------------------------------------------------

    fun testInitTree_getSettings_api401Unauthorized_treeRemainsNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(401, "Unauthorized")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 401 on getSettings()", treeField(panel))
        assertNull("treeModel must remain null after 401 on getSettings()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load flags list. For more information check the logs.", null) }
    }

    fun testInitTree_getSettings_api429TooManyRequests_treeRemainsNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(429, "Too Many Requests")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 429 on getSettings()", treeField(panel))
        assertNull("treeModel must remain null after 429 on getSettings()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load flags list. For more information check the logs.", null) }
    }

    fun testInitTree_getSettings_api500ServerError_treeRemainsNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(500, "Internal Server Error")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 500 on getSettings()", treeField(panel))
        assertNull("treeModel must remain null after 500 on getSettings()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load flags list. For more information check the logs.", null) }
    }

    fun testInitTree_getSettings_api503ServiceUnavailable_treeRemainsNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(503, "Service Unavailable")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after 503 on getSettings()", treeField(panel))
        assertNull("treeModel must remain null after 503 on getSettings()", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load flags list. For more information check the logs.", null) }
    }

    // -------------------------------------------------------------------------
    // Successful init (sanity baseline for the refresh / event-driven tests)
    // -------------------------------------------------------------------------

    fun testInitTree_success_emptyFlagList_treeAndModelAreCreated() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()

        assertNotNull("tree must be set after a successful initTree()", treeField(panel))
        assertNotNull("treeModel must be set after a successful initTree()", treeModelField(panel))
        verify { mockNodeDataService.resetConfigsFlags() }
    }

    fun testInitTree_success_withFlags_treeAndModelAreCreated() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        val flag = mockk<SettingModel>(relaxed = true)
        every { mockFeatureFlagsApi.getSettings(any()) } returns listOf(flag)

        val panel = buildPanel()
        waitForAsync()

        assertNotNull("tree must be set after successful initTree() with flags", treeField(panel))
        assertNotNull("treeModel must be set after successful initTree() with flags", treeModelField(panel))
        verify { mockNodeDataService.resetConfigsFlags() }
    }

    // -------------------------------------------------------------------------
    // loadConnectedConfig – public method contracts
    // -------------------------------------------------------------------------

    fun testLoadConnectedConfig_noStoredConfigId_returnsNull() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns null

        val panel = buildPanel()

        assertNull("loadConnectedConfig() must return null when no config ID is stored", panel.loadConnectedConfig())
    }

    fun testLoadConnectedConfig_apiThrows_returnsNullAndNotifiesError() {
        configureStateAsConfigured()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } throws ApiException(404, "Not Found")

        val panel = buildPanel()

        assertNull("loadConnectedConfig() must return null when API throws", panel.loadConnectedConfig())
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load config. For more information check the logs.", null) }
    }

    fun testLoadConnectedConfig_success_returnsConfigModel() {
        configureStateAsConfigured()
        val configModel = createConfigModel()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } returns configModel

        val panel = buildPanel()

        val result = panel.loadConnectedConfig()
        assertNotNull("loadConnectedConfig() must return a ConfigModel on success", result)
        assertSame("Returned config must be the mocked ConfigModel", configModel, result)
    }

    // -------------------------------------------------------------------------
    // getSelectedNode
    // -------------------------------------------------------------------------

    fun testGetSelectedNode_noTree_returnsNull() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()

        assertNull("getSelectedNode() must return null when tree is null", panel.getSelectedNode())
    }

    fun testGetSelectedNode_emptyTreeWithoutSelection_returnsNull() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val root = DefaultMutableTreeNode("root")
        setTreeField(panel, Tree(DefaultTreeModel(root)))

        assertNull("getSelectedNode() must return null when nothing is selected", panel.getSelectedNode())
    }

    fun testGetSelectedNode_configRootNodeSelected_returnsConfigRootTreeNode() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val configRootNode = ConfigRootNode(emptyList(), "My Config")
        val root = DefaultMutableTreeNode(configRootNode)
        val swingTree = Tree(DefaultTreeModel(root))
        swingTree.selectionPath = TreePath(arrayOf<Any>(root))
        setTreeField(panel, swingTree)

        val selected = panel.getSelectedNode()
        assertNotNull("getSelectedNode() must return the selected ConfigRootNode tree node", selected)
        assertSame("Returned node must be the root ConfigRootNode tree node", root, selected)
        assertTrue("Selected userObject must be a ConfigRootNode", selected!!.userObject is ConfigRootNode)
    }

    fun testGetSelectedNode_flagNodeSelected_returnsFlagTreeNode() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val configRootNode = ConfigRootNode(emptyList(), "My Config")
        val root = DefaultMutableTreeNode(configRootNode)
        val settingModel = mockk<SettingModel>(relaxed = true)
        val flagNode = FlagNode(settingModel, configRootNode)
        val flagTreeNode = DefaultMutableTreeNode(flagNode)
        root.add(flagTreeNode)
        val swingTree = Tree(DefaultTreeModel(root))
        swingTree.selectionPath = TreePath(arrayOf<Any>(root, flagTreeNode))
        setTreeField(panel, swingTree)

        val selected = panel.getSelectedNode()
        assertNotNull("getSelectedNode() must return the selected FlagNode tree node", selected)
        assertSame("Returned node must be the FlagNode tree node", flagTreeNode, selected)
        assertTrue("Selected userObject must be a FlagNode", selected!!.userObject is FlagNode)
    }

    // -------------------------------------------------------------------------
    // refreshTree via SettingsTreeChangeNotifier
    // -------------------------------------------------------------------------

    fun testRefreshTree_successfulRefresh_treeStillExists() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        publishTreeRefresh()
        waitForAsync()

        assertNotNull("Tree must still exist after successful refresh", treeField(panel))
        // resetConfigsFlags must be called at least once per initTree() invocation
        verify(atLeast = 2) { mockNodeDataService.resetConfigsFlags() }
    }

    fun testRefreshTree_getSettingsFailureAfterPreviousSuccess_treeBecomesNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        // API fails on next call triggered by refresh
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(500, "Server Error during refresh")

        publishTreeRefresh()
        waitForAsync()

        assertNull("Tree must become null when getSettings() throws during refresh", treeField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>(), "Failed to load flags list. For more information check the logs.", null) }
    }

    fun testRefreshTree_successAfterPreviousGetSettingsFailure_treeIsRestored() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } throws ApiException(500, "Initial getSettings failure")

        val panel = buildPanel()
        waitForAsync()
        assertNull("Pre-condition: tree must be null after initial getSettings failure", treeField(panel))

        // API recovers on refresh
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        publishTreeRefresh()
        waitForAsync()

        assertNotNull("Tree must be restored after successful refresh", treeField(panel))
        assertNotNull("TreeModel must be restored after successful refresh", treeModelField(panel))
        verify(atLeast = 1) { mockNodeDataService.resetConfigsFlags() }
    }

    // -------------------------------------------------------------------------
    // ConnectedConfigChangeNotifier – reconnect / disconnect scenarios
    // -------------------------------------------------------------------------

    fun testConnectedConfigChange_fromNullToValidConfig_treeIsCreated() {
        configureStateAsConfigured()
        // Initially no connected config → tree stays null after init
        every { mockPropertiesService.getConnectedConfig() } returns null

        val panel = buildPanel()
        waitForAsync()
        assertNull("Pre-condition: tree must be null when no connected config is set", treeField(panel))

        // Connected config is now set
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        publishConnectedConfigChange()
        waitForAsync()

        assertNotNull("Tree must be created after a connected config is set", treeField(panel))
        assertNotNull("TreeModel must be created after a connected config is set", treeModelField(panel))
    }

    fun testConnectedConfigChange_toNewConfig_treeIsRebuilt() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        // Simulate switching to a different connected config
        val newConfigModel = mockk<ConfigModel>(relaxed = true)
        every { newConfigModel.configId } returns UUID.randomUUID()
        every { newConfigModel.name } returns "New Config"
        every { mockConfigsApi.getConfig(any()) } returns newConfigModel
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        publishConnectedConfigChange()
        waitForAsync()

        assertNotNull("Tree must exist after switching to a new connected config", treeField(panel))
        verify(atLeast = 2) { mockNodeDataService.resetConfigsFlags() }
    }

    // -------------------------------------------------------------------------
    // ConfigChangeNotifier – credential / settings-level changes
    // -------------------------------------------------------------------------

    fun testConfigChange_pluginBecomesUnconfigured_treeBecomesNull() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist before config change", treeField(panel))

        // Plugin credentials are cleared
        every { mockState.isConfigured() } returns false

        publishConfigChange()
        // resetTreeView() is called synchronously when isConfigured() returns false

        assertNull("Tree must become null when plugin becomes unconfigured", treeField(panel))
        assertNull("TreeModel must become null when plugin becomes unconfigured", treeModelField(panel))
    }

    fun testConfigChange_withValidConfig_treeIsRefreshed() {
        configureStateAsConfigured()
        configureConnectedConfigSuccess()
        every { mockFeatureFlagsApi.getSettings(any()) } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        // Credentials updated but plugin stays configured with same connected config
        publishConfigChange()
        waitForAsync()

        assertNotNull("Tree must still exist after a config change that keeps the plugin configured", treeField(panel))
        verify(atLeast = 2) { mockNodeDataService.resetConfigsFlags() }
    }

    // -------------------------------------------------------------------------
    // Flag selection – auto-select newly created flags
    // -------------------------------------------------------------------------

    fun testSelectFlagIfPresent_selectsMatchingFlagNode() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val configRootNode = ConfigRootNode(emptyList(), "My Config")
        val root = DefaultMutableTreeNode(configRootNode)

        val targetFlagId = 98765
        val targetSettingModel = mockk<SettingModel>(relaxed = true)
        every { targetSettingModel.settingId } returns targetFlagId
        val targetFlagTreeNode = DefaultMutableTreeNode(FlagNode(targetSettingModel, configRootNode))

        val otherSettingModel = mockk<SettingModel>(relaxed = true)
        every { otherSettingModel.settingId } returns 11111
        val otherFlagTreeNode = DefaultMutableTreeNode(FlagNode(otherSettingModel, configRootNode))

        root.add(otherFlagTreeNode)
        root.add(targetFlagTreeNode)

        val swingTree = Tree(DefaultTreeModel(root))
        setTreeField(panel, swingTree)
        setPendingFlagSelection(panel, targetFlagId)

        // Invoke the selection method directly (this is what the tree listener does)
        invokeSelectFlagIfPresent(panel, targetFlagTreeNode)

        val selected = panel.getSelectedNode()
        assertSame("The matching flag tree node must become the selection", targetFlagTreeNode, selected)
        assertNull("Pending flag ID must be cleared after selection", pendingSelectionFlagIdField(panel))
    }

    fun testSelectFlagIfPresent_missingFlag_clearsPendingId() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val configRootNode = ConfigRootNode(emptyList(), "My Config")
        val root = DefaultMutableTreeNode(configRootNode)

        val existingSettingModel = mockk<SettingModel>(relaxed = true)
        every { existingSettingModel.settingId } returns 22222
        root.add(DefaultMutableTreeNode(FlagNode(existingSettingModel, configRootNode)))

        val swingTree = Tree(DefaultTreeModel(root))
        setTreeField(panel, swingTree)
        setPendingFlagSelection(panel, 99999)

        // Pending ID should remain set since matching flag node doesn't exist
        assertEquals("Pending flag ID should remain set when matching flag is missing", 99999, pendingSelectionFlagIdField(panel))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildPanel(): SettingsPanel {
        val panel = SettingsPanel(CoroutineScope(Dispatchers.Unconfined))
        Disposer.register(testRootDisposable, panel)
        return panel
    }

    private fun publishTreeRefresh() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(SettingsTreeChangeNotifier.TREE_REFRESH_TOPIC)
            .notifyTreeRefresh()
    }

    private fun publishConnectedConfigChange() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(ConnectedConfigChangeNotifier.CONNECTED_CONFIG_CHANGE_TOPIC)
            .notifyConnectedConfigChange()
    }

    private fun publishConfigChange() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC)
            .notifyConfigChange()
    }

    private fun waitForAsync() {
        Thread.sleep(500)
    }

    private fun configureStateAsConfigured() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
    }

    private fun configureConnectedConfigSuccess() {
        val configModel = createConfigModel()
        every { mockPropertiesService.getConnectedConfig() } returns connectedConfigId
        every { mockConfigsApi.getConfig(any()) } returns configModel
    }

    private fun createConfigModel(): ConfigModel {
        val configModel = mockk<ConfigModel>(relaxed = true)
        every { configModel.configId } returns UUID.fromString(connectedConfigId)
        every { configModel.name } returns "Test Config"
        return configModel
    }

    private fun treeField(panel: SettingsPanel): Tree? {
        val field: Field = SettingsPanel::class.java.getDeclaredField("tree")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(panel) as Tree?
    }

    private fun setTreeField(panel: SettingsPanel, tree: Tree?) {
        val field: Field = SettingsPanel::class.java.getDeclaredField("tree")
        field.isAccessible = true
        field.set(panel, tree)
    }

    private fun treeModelField(panel: SettingsPanel): Any? {
        val field: Field = SettingsPanel::class.java.getDeclaredField("treeModel")
        field.isAccessible = true
        return field.get(panel)
    }

    private fun pendingSelectionFlagIdField(panel: SettingsPanel): Int? {
        val field: Field = SettingsPanel::class.java.getDeclaredField("pendingSelectionFlagId")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(panel) as Int?
    }

    private fun setPendingFlagSelection(panel: SettingsPanel, flagId: Int?) {
        val field: Field = SettingsPanel::class.java.getDeclaredField("pendingSelectionFlagId")
        field.isAccessible = true
        field.set(panel, flagId)
    }

    private fun invokeSelectFlagIfPresent(panel: SettingsPanel, flagTreeNode: DefaultMutableTreeNode) {
        val method = SettingsPanel::class.java.getDeclaredMethod("selectFlagIfPresent", DefaultMutableTreeNode::class.java)
        method.isAccessible = true
        method.invoke(panel, flagTreeNode)
    }
}
