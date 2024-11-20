package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.ui.treeStructure.SimpleNode

class RootNode(private val products: List<ProductModel>): SimpleNode() {

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

class ProductNode(val product: ProductModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = product.description
    }

    override fun getChildren(): Array<SimpleNode> {
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
        val productId = product.productId
        val configs: List<ConfigModel>? = if(productId == null) {
           emptyList()
        }  else {
            configCatNodeDataService.getProductConfigs(productId)
        }
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
        return product.name ?: product.productId?.toString() ?: "<missing data>"
    }

}

class ConfigNode(val config: ConfigModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = config.description
    }

    override fun getChildren(): Array<SimpleNode> {
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()

        val configId = config.configId
        val flags: List<SettingModel>? = if(configId == null) {
            emptyList()
        }  else {
            configCatNodeDataService.getConfigFlags(configId)
        }

        if(flags == null) {
            return arrayOf(InfoNode("Loading..."))
        } else {
            if(flags.isEmpty()) {
                return arrayOf(InfoNode("No flags."))
            } else {
                val flagNodes: MutableList<SimpleNode> = ArrayList()
                for (flag in flags) {
                    flagNodes.add(FlagNode(flag, this))
                }
                return flagNodes.toTypedArray()
            }
        }
    }

    override fun getName(): String {
        return config.name ?: config.configId?.toString() ?: "<missing data>"
    }

}

class FlagNode(val setting: SettingModel, parent: SimpleNode): SimpleNode(null, parent) {

    init {
        presentation.tooltip = setting.hint
    }

    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

    override fun getName(): String {
        return if(setting.name?.isEmpty() != false && setting.key?.isEmpty() != false)  "<missing data>" else setting.name + " (${setting.key})"
    }
}

class InfoNode(private val message: String): SimpleNode() {

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