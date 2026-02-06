package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode


interface TreeChangeNotifier {

    fun notifyTreeRefresh()

    fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {}

    companion object {
        @Topic.AppLevel
        val TREE_REFRESH_TOPIC: Topic<TreeChangeNotifier> = Topic.create(
            "ConfigCat Tree Refresh",
            TreeChangeNotifier::class.java
        )
    }
}