package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode

interface ProductsConfigsTreeChangeNotifier {

    fun notifyTreeRefresh()

    fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {}

    companion object {
        @Topic.AppLevel
        val TREE_REFRESH_TOPIC: Topic<ProductsConfigsTreeChangeNotifier> = Topic.create(
            "ConfigCat Products & Configs Tree Refresh",
            ProductsConfigsTreeChangeNotifier::class.java
        )
    }
}