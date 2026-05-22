package com.configcat.intellij.plugin.toolWindow.tree

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.ide.projectView.PresentationData
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.SimpleTextAttributes
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID

class ConfigCatNodesTest : LightPlatformTestCase() {

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testProductRootNodeWithEmptyProductsReturnsInfoNode() {
        val root = ProductRootNode(emptyList())

        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("No products.", children[0].name)
        assertTrue(root.isAutoExpandNode)
        assertEquals("ConfigCat Products", root.name)
    }

    fun testProductRootNodeWithProductsReturnsProductNodesAndAccumulatesOnRepeatedCalls() {
        val productA = createProductModel(name = "Product A")
        val productB = createProductModel(name = "Product B")
        val root = ProductRootNode(listOf(productA, productB))

        val firstChildren = root.children

        assertEquals(2, firstChildren.size)
        assertTrue(firstChildren.all { it is ProductNode })
        assertEquals(listOf("Product A", "Product B"), firstChildren.map { it.name })
    }

    fun testConfigRootNodeWithEmptyFlagsReturnsInfoNode() {
        val root = ConfigRootNode(emptyList(), "Test Config")

        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("No flags.", children[0].name)
        assertEquals("Test Config", root.name)
    }

    fun testConfigRootNodeWithFlagsReturnsFlagNodesAndAccumulatesOnRepeatedCalls() {
        val flagA = createSettingModel(name = "Flag A", key = "a")
        val flagB = createSettingModel(name = "Flag B", key = "b")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        val firstChildren = root.children

        assertEquals(2, firstChildren.size)
        assertTrue(firstChildren.all { it is FlagNode })
        assertEquals(listOf("Flag A (a)", "Flag B (b)"), firstChildren.map { it.name })
    }

    fun testConfigRootNodeFilterByNameReturnsOnlyMatchingNode() {
        val flagA = createSettingModel(name = "Alpha Flag", key = "a")
        val flagB = createSettingModel(name = "Beta Flag", key = "b")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        root.filterQuery = "alpha"
        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is FlagNode)
        assertEquals("Alpha Flag (a)", children[0].name)
    }

    fun testConfigRootNodeFilterByKeyReturnsOnlyMatchingNode() {
        val flagA = createSettingModel(name = "Alpha Flag", key = "alpha_key")
        val flagB = createSettingModel(name = "Beta Flag", key = "beta_key")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        root.filterQuery = "beta_key"
        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is FlagNode)
        assertEquals("Beta Flag (beta_key)", children[0].name)
    }

    fun testConfigRootNodeFilterIsCaseInsensitive() {
        val flagA = createSettingModel(name = "Alpha Flag", key = "a")
        val flagB = createSettingModel(name = "Beta Flag", key = "b")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        root.filterQuery = "ALPHA"
        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is FlagNode)
        assertEquals("Alpha Flag (a)", children[0].name)
    }

    fun testConfigRootNodeFilterWithNoMatchReturnsNoMatchingFlagInfoNode() {
        val flagA = createSettingModel(name = "Alpha Flag", key = "a")
        val flagB = createSettingModel(name = "Beta Flag", key = "b")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        root.filterQuery = "xyz"
        val children = root.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("No matching flag.", children[0].name)
    }

    fun testConfigRootNodeFilterClearedReturnsAllNodes() {
        val flagA = createSettingModel(name = "Alpha Flag", key = "a")
        val flagB = createSettingModel(name = "Beta Flag", key = "b")
        val root = ConfigRootNode(listOf(flagA, flagB), "Test Config")

        root.filterQuery = "alpha"
        assertEquals(1, root.children.size)

        root.filterQuery = ""
        val children = root.children

        assertEquals(2, children.size)
        assertTrue(children.all { it is FlagNode })
    }

    fun testProductNodeWithNullConfigsReturnsLoadingInfoNode() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val product = createProductModel(name = "Product A")
        every { nodeDataService.getProductConfigs(product.productId) } returns null

        val node = ProductNode(product, ProductRootNode(emptyList()))

        val children = node.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("Loading...", children[0].name)
        assertEquals("Product A", node.name)
        assertTrue(node.isAutoExpandNode)
    }

    fun testProductNodeWithEmptyConfigsReturnsNoConfigsInfoNode() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val product = createProductModel(name = "Product A")
        every { nodeDataService.getProductConfigs(product.productId) } returns emptyList()

        val node = ProductNode(product, ProductRootNode(emptyList()))

        val children = node.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("No configs.", children[0].name)
    }

    fun testProductNodeWithConfigsReturnsConfigNodes() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val product = createProductModel(name = "Product A")
        val configA = createConfigModel(name = "Config A")
        val configB = createConfigModel(name = "Config B")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configA, configB)

        val node = ProductNode(product, ProductRootNode(emptyList()))

        val children = node.children

        assertEquals(2, children.size)
        assertTrue(children.all { it is ConfigNode })
        assertEquals(listOf("Config A", "Config B"), children.map { it.name })
        verify(exactly = 1) { nodeDataService.getProductConfigs(product.productId) }
    }

    fun testConfigNodeReturnsNameAndNoChildren() {
        val product = createProductModel()
        val config = createConfigModel(name = "Config A")
        val node = ConfigNode(config, ProductNode(product, ProductRootNode(emptyList())))

        assertEquals("Config A", node.name)
        assertTrue(node.isAutoExpandNode)
        assertEquals(0, node.children.size)
    }

    fun testFlagNodeWithMissingDataRendersFallbackText() {
        val setting = createSettingModel(name = "", key = "")
        val node = FlagNode(setting, ConfigRootNode(emptyList(), "Config"))
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        verify(exactly = 1) {
            presentation.addText("<missing data>", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
        }
        assertEquals("<missing data>", node.name)
        assertTrue(node.isAlwaysLeaf)
        assertEquals(0, node.children.size)
    }

    fun testFlagNodeWithDataRendersNameAndKey() {
        val setting = createSettingModel(name = "My Flag", key = "my_flag_key")
        val node = FlagNode(setting, ConfigRootNode(emptyList(), "Config"))
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        verify(exactly = 1) {
            presentation.addText("My Flag", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
        verify(exactly = 1) {
            presentation.addText(" my_flag_key", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
        }
        assertEquals("My Flag (my_flag_key)", node.name)
    }

    fun testFlagNodeHighlightsMatchInNameOnly() {
        val setting = createSettingModel(name = "My Flag", key = "some_key")
        val root = ConfigRootNode(emptyList(), "Config")
        root.filterQuery = "flag"
        val node = FlagNode(setting, root)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        val highlightAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD or SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
        verify(exactly = 1) { presentation.addText("My ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("Flag", highlightAttrs) }
        verify(exactly = 1) { presentation.addText(" ", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("some_key", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
    }

    fun testFlagNodeHighlightsMatchInKeyOnly() {
        val setting = createSettingModel(name = "My Flag", key = "my_key_abc")
        val root = ConfigRootNode(emptyList(), "Config")
        root.filterQuery = "key"
        val node = FlagNode(setting, root)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        val highlightAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC or SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
        verify(exactly = 1) { presentation.addText("My Flag", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText(" ", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("my_", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("key", highlightAttrs) }
        verify(exactly = 1) { presentation.addText("_abc", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
    }

    fun testFlagNodeHighlightsMultipleMatchesInName() {
        val setting = createSettingModel(name = "ab yes ab", key = "k")
        val root = ConfigRootNode(emptyList(), "Config")
        root.filterQuery = "ab"
        val node = FlagNode(setting, root)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        val highlightAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD or SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
        verify(exactly = 2) { presentation.addText("ab", highlightAttrs) }
        verify(exactly = 1) { presentation.addText(" yes ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText(" ", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("k", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
    }

    fun testFlagNodeMissingDataNotAffectedByFilter() {
        val setting = createSettingModel(name = "", key = "")
        val root = ConfigRootNode(emptyList(), "Config")
        root.filterQuery = "something"
        val node = FlagNode(setting, root)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        verify(exactly = 1) { presentation.addText("<missing data>", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
        verify(exactly = 0) { presentation.addText(any(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) }
    }

    // -------------------------------------------------------------------------
    // ProductRootNode – filter tests
    // -------------------------------------------------------------------------

    fun testProductRootNodeFilterByLoadedConfigName_returnsNotModifiedProducts() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val productA = createProductModel(name = "Alpha Product")
        val productB = createProductModel(name = "Beta Product")
        val configWithMatch = createConfigModel(name = "Special Config")
        every { nodeDataService.getProductConfigs(productA.productId) } returns emptyList()
        every { nodeDataService.getProductConfigs(productB.productId) } returns listOf(configWithMatch)

        val root = ProductRootNode(listOf(productA, productB))

        root.filterQuery = "Special"
        val children = root.children

        assertEquals(2, children.size)
    }

    // -------------------------------------------------------------------------
    // ProductNode – filter tests (getChildren)
    // -------------------------------------------------------------------------

    fun testProductNodeFilterByProductNameMatch_returnsAllConfigs() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val root = ProductRootNode(emptyList())
        root.filterQuery = "product"
        val product = createProductModel(name = "My Product")
        val configA = createConfigModel(name = "Config A")
        val configB = createConfigModel(name = "Config B")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configA, configB)

        val node = ProductNode(product, root)
        val children = node.children

        assertEquals(2, children.size)
        assertTrue(children.all { it is ConfigNode })
        assertEquals(listOf("Config A", "Config B"), children.map { it.name })
    }

    fun testProductNodeFilterByConfigName_returnsOnlyMatchingConfigs() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val root = ProductRootNode(emptyList())
        root.filterQuery = "Special"
        val product = createProductModel(name = "My Product")
        val configA = createConfigModel(name = "Special Config")
        val configB = createConfigModel(name = "Other Config")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configA, configB)

        val node = ProductNode(product, root)
        val children = node.children

        assertEquals(1, children.size)
        assertTrue(children[0] is ConfigNode)
        assertEquals("Special Config", children[0].name)
    }

    fun testProductNodeFilterNoMatch_returnsNoMatchingConfigInfoNode() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val root = ProductRootNode(emptyList())
        root.filterQuery = "xyz"
        val product = createProductModel(name = "My Product")
        val configA = createConfigModel(name = "Config A")
        val configB = createConfigModel(name = "Config B")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configA, configB)

        val node = ProductNode(product, root)
        val children = node.children

        assertEquals(1, children.size)
        assertTrue(children[0] is InfoNode)
        assertEquals("No matching config.", children[0].name)
    }

    fun testProductNodeFilterIsCaseInsensitive() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val root = ProductRootNode(emptyList())
        root.filterQuery = "SPECIAL"
        val product = createProductModel(name = "My Product")
        val configMatch = createConfigModel(name = "Special Config")
        val configOther = createConfigModel(name = "Other Config")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configMatch, configOther)

        val node = ProductNode(product, root)
        val children = node.children

        assertEquals(1, children.size)
        assertEquals("Special Config", children[0].name)
    }

    fun testProductNodeFilterClearedReturnsAllConfigs() {
        val nodeDataService = mockk<ConfigCatNodeDataService>()
        mockkObject(ConfigCatNodeDataService.Companion)
        every { ConfigCatNodeDataService.getInstance() } returns nodeDataService

        val root = ProductRootNode(emptyList())
        val product = createProductModel(name = "My Product")
        val configA = createConfigModel(name = "Config A")
        val configB = createConfigModel(name = "Config B")
        every { nodeDataService.getProductConfigs(product.productId) } returns listOf(configA, configB)

        root.filterQuery = "Config A"
        val node = ProductNode(product, root)
        assertEquals(1, node.children.size)

        root.filterQuery = ""
        val children = node.children

        assertEquals(2, children.size)
        assertTrue(children.all { it is ConfigNode })
    }

    // -------------------------------------------------------------------------
    // ConfigNode – doUpdate highlight tests
    // -------------------------------------------------------------------------

    fun testConfigNodeDoUpdate_noQuery_rendersConfigName() {
        val product = createProductModel()
        val config = createConfigModel(name = "My Config")
        val root = ProductRootNode(emptyList())
        val productNode = ProductNode(product, root)
        val node = ConfigNode(config, productNode)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        verify(exactly = 1) { presentation.addText("My Config", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
        verify(exactly = 0) {
            presentation.addText(any(), SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null))
        }
    }

    fun testConfigNodeDoUpdate_withQuery_highlightsMatchInName() {
        val product = createProductModel()
        val config = createConfigModel(name = "My Config")
        val root = ProductRootNode(emptyList())
        root.filterQuery = "Config"
        val productNode = ProductNode(product, root)
        val node = ConfigNode(config, productNode)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        val highlightAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
        verify(exactly = 1) { presentation.addText("My ", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
        verify(exactly = 1) { presentation.addText("Config", highlightAttrs) }
    }

    fun testConfigNodeDoUpdate_queryNoMatch_rendersFullNameNormally() {
        val product = createProductModel()
        val config = createConfigModel(name = "My Config")
        val root = ProductRootNode(emptyList())
        root.filterQuery = "xyz"
        val productNode = ProductNode(product, root)
        val node = ConfigNode(config, productNode)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        verify(exactly = 1) { presentation.addText("My Config", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
        verify(exactly = 0) {
            presentation.addText(any(), SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null))
        }
    }

    fun testConfigNodeDoUpdate_multipleMatchesInName_highlightsAll() {
        val product = createProductModel()
        val config = createConfigModel(name = "ab test ab")
        val root = ProductRootNode(emptyList())
        root.filterQuery = "ab"
        val productNode = ProductNode(product, root)
        val node = ConfigNode(config, productNode)
        val presentation = mockk<PresentationData>(relaxed = true)

        invokeDoUpdate(node, presentation)

        val highlightAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
        verify(exactly = 2) { presentation.addText("ab", highlightAttrs) }
        verify(exactly = 1) { presentation.addText(" test ", SimpleTextAttributes.REGULAR_ATTRIBUTES) }
    }

    private fun invokeDoUpdate(node: Any, presentation: PresentationData) {
        val method = node.javaClass.getDeclaredMethod("doUpdate", PresentationData::class.java)
        method.isAccessible = true
        method.invoke(node, presentation)
    }

    private fun createProductModel(
        productId: UUID = UUID.randomUUID(),
        name: String = "Product",
        description: String = "Product description"
    ): ProductModel {
        val product = mockk<ProductModel>()
        every { product.productId } returns productId
        every { product.name } returns name
        every { product.description } returns description
        return product
    }

    private fun createConfigModel(
        name: String = "Config",
        description: String = "Config description"
    ): ConfigModel {
        val config = mockk<ConfigModel>()
        every { config.name } returns name
        every { config.description } returns description
        return config
    }

    private fun createSettingModel(
        name: String,
        key: String,
        hint: String = ""
    ): SettingModel {
        val setting = mockk<SettingModel>()
        every { setting.name } returns name
        every { setting.key } returns key
        every { setting.hint } returns hint
        return setting
    }
}
