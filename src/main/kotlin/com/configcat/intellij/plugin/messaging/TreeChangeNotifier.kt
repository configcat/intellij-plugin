package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode


interface TreeChangeNotifier {

    fun notifyTreeRefresh()

    fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {}

    companion object {
        @Topic.AppLevel
        val TREE_CHANGE_TOPIC: Topic<TreeChangeNotifier> = Topic.create(
            "ConfigCat Config Changed",
            TreeChangeNotifier::class.java
        )
    }
}