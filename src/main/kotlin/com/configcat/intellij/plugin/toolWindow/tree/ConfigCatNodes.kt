package com.configcat.intellij.plugin.toolWindow.tree

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.jetbrains.rd.generator.nova.PredefinedType
import kotlinx.coroutines.launch

class ProductRootNode(private val products: List<ProductModel>): SimpleNode() {

    private var productNodes: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if( products.isEmpty()) {
            return arrayOf(InfoNode("No products."))

        }
        for(product in products) {
            productNodes.add(ProductNode(product, this))
        }
        return productNodes.toTypedArray()
    }

    override fun getName(): String {
        return "ConfigCat Products"
    }
}

class ConfigRootNode(val flags: List<SettingModel>, val configName: String): SimpleNode() {

    val flagNodes: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
            if(flags.isEmpty()) {
                val infoNode = InfoNode("No flags.")
                return arrayOf(infoNode)
            } else {
                for (flag in flags) {
                    flagNodes.add(FlagNode(flag, this))
                }
                return flagNodes.toTypedArray()
        }
    }

    override fun getName(): String {
        return configName
    }
}

class ProductNode(val product: ProductModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = product.description
    }

    override fun getChildren(): Array<SimpleNode> {
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
        val productId = product.productId
        val configs: List<ConfigModel>? = configCatNodeDataService.getProductConfigs(productId)
        if(configs == null) {
            return arrayOf(InfoNode("Loading..."))
        } else {
            if(configs.isEmpty()) {
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

class ConfigNode(val config: ConfigModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = config.description
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun getName(): String {
        return config.name
    }

}

class FlagNode(val setting: SettingModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = setting.hint
    }

    override fun doUpdate(presentation: PresentationData) {
        if(setting.name.isEmpty() && setting.key.isEmpty()){
            presentation.addText("<missing data>", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES )
        } else {
            presentation.addText(setting.key, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            presentation.addText(" ${setting.name}", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
        }
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

    override fun getName(): String {
        return if(setting.name.isEmpty() && setting.key.isEmpty())  "<missing data>" else  setting.key + " (${setting.name})"
    }
}

class InfoNode(private val message: String): SimpleNode() {

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