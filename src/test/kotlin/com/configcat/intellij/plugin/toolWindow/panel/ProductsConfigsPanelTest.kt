package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.tree.ConfigNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductRootNode
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.ProductsApi
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
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

@Suppress("UnstableApiUsage")
class ProductsConfigsPanelTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockNodeDataService: ConfigCatNodeDataService
    private lateinit var mockProductsApi: ProductsApi

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        mockNodeDataService = mockk(relaxed = true)
        mockProductsApi = mockk(relaxed = true)

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns mockNodeDataService

        mockkObject(ConfigCatService.Companion)
        every { ConfigCatService.createProductsService(any(), any()) } returns mockProductsApi

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any()) } just Runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // initTree failure cases
    // -------------------------------------------------------------------------

    fun testNotConfigured_treeAndTreeModelAreNull() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()

        assertNull("tree must be null when plugin is not configured", treeField(panel))
        assertNull("treeModel must be null when plugin is not configured", treeModelField(panel))
    }

    fun testInitTree_api401Unauthorized_treeRemainsNull() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } throws ApiException(401, "Unauthorized")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after a 401 ApiException", treeField(panel))
        assertNull("treeModel must remain null after a 401 ApiException", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testInitTree_api429TooManyRequest_treeRemainsNull() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } throws ApiException(429, "Too many requests.")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after a 429 ApiException", treeField(panel))
        assertNull("treeModel must remain null after a 429 ApiException", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testInitTree_api500ServerError_treeRemainsNull() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } throws ApiException(500, "Internal Server Error")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after a 500 ApiException", treeField(panel))
        assertNull("treeModel must remain null after a 500 ApiException", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testInitTree_api503ServiceUnavailable_treeRemainsNull() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } throws ApiException(503, "Service Unavailable")

        val panel = buildPanel()
        waitForAsync()

        assertNull("tree must remain null after a 503 ApiException", treeField(panel))
        assertNull("treeModel must remain null after a 503 ApiException", treeModelField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    // -------------------------------------------------------------------------
    // Successful init (sanity baseline for the refresh tests)
    // -------------------------------------------------------------------------

    fun testInitTree_success_treeAndModelAreCreated() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } returns emptyList()

        val panel = buildPanel()
        waitForAsync()

        assertNotNull("tree must be set after a successful initTree()", treeField(panel))
        assertNotNull("treeModel must be set after a successful initTree()", treeModelField(panel))
        verify { mockNodeDataService.resetProductConfigsData() }
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
        val swingTree = Tree(DefaultTreeModel(root))
        setTreeField(panel, swingTree)

        assertNull("getSelectedNode() must return null when tree has no selected node", panel.getSelectedNode())
    }

    fun testGetSelectedNode_productNodeSelection_returnsProductTreeNode() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val productNode = createProductNodeForTest()
        val root = DefaultMutableTreeNode("root")
        val productTreeNode = DefaultMutableTreeNode(productNode)
        root.add(productTreeNode)
        val swingTree = Tree(DefaultTreeModel(root))
        swingTree.selectionPath = TreePath(arrayOf(root, productTreeNode))
        setTreeField(panel, swingTree)

        val selectedNode = panel.getSelectedNode()
        assertNotNull("getSelectedNode() must return the selected ProductNode", selectedNode)
        assertSame("Returned node must be the selected ProductNode tree node", productTreeNode, selectedNode)
        assertTrue("Selected userObject must be ProductNode", selectedNode!!.userObject is ProductNode)
    }

    fun testGetSelectedNode_configNodeSelection_returnsConfigTreeNode() {
        every { mockState.isConfigured() } returns false

        val panel = buildPanel()
        val productNode = createProductNodeForTest()
        val configNode = createConfigNodeForTest(productNode)
        val root = DefaultMutableTreeNode("root")
        val productTreeNode = DefaultMutableTreeNode(productNode)
        val configTreeNode = DefaultMutableTreeNode(configNode)
        root.add(productTreeNode)
        productTreeNode.add(configTreeNode)
        val swingTree = Tree(DefaultTreeModel(root))
        swingTree.selectionPath = TreePath(arrayOf(root, productTreeNode, configTreeNode))
        setTreeField(panel, swingTree)

        val selectedNode = panel.getSelectedNode()
        assertNotNull("getSelectedNode() must return the selected ConfigNode", selectedNode)
        assertSame("Returned node must be the selected ConfigNode tree node", configTreeNode, selectedNode)
        assertTrue("Selected userObject must be ConfigNode", selectedNode!!.userObject is ConfigNode)
    }

    // -------------------------------------------------------------------------
    // refreshTree failure cases (triggered via the message bus)
    // -------------------------------------------------------------------------

    fun testRefreshTree_successfulRefresh_treeBecomeNullThenRestored() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        // Refresh should trigger a new API call which succeeds
        publishTreeRefresh()
        waitForAsync()

        assertNotNull("Tree must still exist after successful refresh", treeField(panel))
        // resetProductConfigsData should be called at least twice: once for init, once for refresh
        verify(atLeast = 2) { mockNodeDataService.resetProductConfigsData() }
    }

    fun testRefreshTree_failureAfterPreviousSuccess_treeBecomesNull() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } returns emptyList()

        val panel = buildPanel()
        waitForAsync()
        assertNotNull("Pre-condition: tree must exist after initial success", treeField(panel))

        // Make the API fail on the next call (triggered by refresh)
        every { mockProductsApi.products } throws ApiException(500, "Server Error during refresh")

        publishTreeRefresh()
        waitForAsync()

        assertNull("Tree must become null after failed refresh", treeField(panel))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testRefreshTree_successAfterPreviousFailure_treeIsRestored() {
        every { mockState.isConfigured() } returns true
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"
        every { mockProductsApi.products } throws ApiException(500, "Initial failure")

        val panel = buildPanel()
        waitForAsync()
        assertNull("Pre-condition: tree must be null after initial API failure", treeField(panel))

        // Make the API succeed on the next call (triggered by refresh)
        every { mockProductsApi.products } returns emptyList()

        publishTreeRefresh()
        waitForAsync()

        assertNotNull("Tree must be restored after successful refresh", treeField(panel))
        assertNotNull("TreeModel must be restored after successful refresh", treeModelField(panel))
        verify(atLeast = 1) { mockNodeDataService.resetProductConfigsData() }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildPanel(): ProductsConfigsPanel {
        val panel = ProductsConfigsPanel(CoroutineScope(Dispatchers.Unconfined))
        Disposer.register(testRootDisposable, panel)
        return panel
    }

    private fun publishTreeRefresh() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC)
            .notifyTreeRefresh()
    }

    private fun waitForAsync() {
        // Give coroutines time to execute on Dispatchers.Default and Dispatchers.EDT
        Thread.sleep(200)
    }

    private fun treeField(panel: ProductsConfigsPanel): Tree? {
        val field: Field = ProductsConfigsPanel::class.java.getDeclaredField("tree")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(panel) as Tree?
    }

    private fun setTreeField(panel: ProductsConfigsPanel, tree: Tree?) {
        val field: Field = ProductsConfigsPanel::class.java.getDeclaredField("tree")
        field.isAccessible = true
        field.set(panel, tree)
    }

    private fun createProductNodeForTest(): ProductNode {
        val productModel = mockk<ProductModel>(relaxed = true)
        every { productModel.description } returns "product description"
        every { productModel.name } returns "Product A"
        every { productModel.productId } returns UUID.randomUUID()
        return ProductNode(productModel, ProductRootNode(emptyList()))
    }

    private fun createConfigNodeForTest(parent: ProductNode): ConfigNode {
        val configModel = mockk<ConfigModel>(relaxed = true)
        every { configModel.description } returns "config description"
        every { configModel.name } returns "Config A"
        return ConfigNode(configModel, parent)
    }

    private fun treeModelField(panel: ProductsConfigsPanel): Any? {
        val field: Field = ProductsConfigsPanel::class.java.getDeclaredField("treeModel")
        field.isAccessible = true
        return field.get(panel)
    }
}







