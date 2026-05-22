package com.configcat.intellij.plugin.toolWindow.tree

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode

class ProductRootNode(private val products: List<ProductModel>) : SimpleNode() {
    private var productNodes: MutableList<SimpleNode> = ArrayList()

    init {
        for (product in products) {
            productNodes.add(ProductNode(product, this))
        }
    }

    override fun isAutoExpandNode(): Boolean {
        return true
    }

    override fun getChildren(): Array<SimpleNode> {
        if (productNodes.isEmpty()) {
            return arrayOf(InfoNode("No products."))
        }
        return productNodes.toTypedArray()
    }

    override fun getName(): String {
        return "ConfigCat Products"
    }
}

class ConfigRootNode(val flags: List<SettingModel>, val configName: String) : SimpleNode() {

    val flagNodes: MutableList<SimpleNode> = ArrayList()
    var filterQuery: String = ""

    init {
        for (flag in flags) {
            flagNodes.add(FlagNode(flag, this))
        }
    }

    override fun getChildren(): Array<SimpleNode> {
        val query = filterQuery.trim()
        if (query.isBlank()) {
            if (flagNodes.isEmpty()) return arrayOf(InfoNode("No flags."))
            return flagNodes.toTypedArray()
        }
        val filtered = flagNodes.filterIsInstance<FlagNode>().filter { node ->
            node.setting.name.contains(query, ignoreCase = true) ||
                node.setting.key.contains(query, ignoreCase = true)
        }
        if (filtered.isEmpty()) return arrayOf(InfoNode("No matching flag."))
        return filtered.toTypedArray()
    }

    override fun getName(): String {
        return configName
    }
}

class ProductNode(val product: ProductModel, parent: SimpleNode) : SimpleNode(null, parent) {

    init {
        presentation.tooltip = product.description
    }

    override fun isAutoExpandNode(): Boolean {
        return true
    }

    override fun getChildren(): Array<SimpleNode> {
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
        val productId = product.productId
        val configs: List<ConfigModel>? = configCatNodeDataService.getProductConfigs(productId)
        if (configs == null) {
            return arrayOf(InfoNode("Loading..."))
        } else {
            if (configs.isEmpty()) {
                return arrayOf(InfoNode("No configs."))
            } else {
                val configNodes: MutableList<SimpleNode> = ArrayList()
                for (config in configs) {
                    configNodes.add(ConfigNode(config, this))
                }
                return configNodes.toTypedArray()
            }
        }
    }

    override fun getName(): String {
        return product.name
    }
}

class ConfigNode(val config: ConfigModel, parent: SimpleNode) : SimpleNode(null, parent) {

    init {
        presentation.tooltip = config.description
    }

    override fun isAutoExpandNode(): Boolean {
        return true
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun getName(): String {
        return config.name
    }

}

class FlagNode(val setting: SettingModel, parentNode: SimpleNode) : SimpleNode(null, parentNode) {

    private val configRoot: ConfigRootNode? = parentNode as? ConfigRootNode

    init {
        presentation.tooltip = setting.hint
    }

    override fun doUpdate(presentation: PresentationData) {
        if (setting.name.isEmpty() && setting.key.isEmpty()) {
            presentation.addText("<missing data>", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
            return
        }
        val query = configRoot?.filterQuery?.trim() ?: ""
        if (query.isBlank()) {
            presentation.addText(setting.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            presentation.addText(" ${setting.key}", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
        } else {
            addHighlightedText(
                presentation, setting.name, query,
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
                SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD or SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
            )
            presentation.addText(" ", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
            addHighlightedText(
                presentation, setting.key, query,
                SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES,
                SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC or SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
            )
        }
    }

    private fun addHighlightedText(
        presentation: PresentationData,
        text: String,
        query: String,
        normalAttrs: SimpleTextAttributes,
        highlightAttrs: SimpleTextAttributes
    ) {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var lastIndex = 0
        var matchIndex = lowerText.indexOf(lowerQuery, lastIndex)
        while (matchIndex != -1) {
            if (matchIndex > lastIndex) {
                presentation.addText(text.substring(lastIndex, matchIndex), normalAttrs)
            }
            presentation.addText(text.substring(matchIndex, matchIndex + query.length), highlightAttrs)
            lastIndex = matchIndex + query.length
            matchIndex = lowerText.indexOf(lowerQuery, lastIndex)
        }
        if (lastIndex < text.length) {
            presentation.addText(text.substring(lastIndex), normalAttrs)
        }
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

    override fun getName(): String {
        return if (setting.name.isEmpty() && setting.key.isEmpty()) {
            "<missing data>"
        } else {
            setting.name + " (${setting.key})"
        }
    }
}

class InfoNode(private val message: String) : SimpleNode() {

    override fun doUpdate(presentation: PresentationData) {
        presentation.addText(message, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun getName(): String {
        return message
    }

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

}
