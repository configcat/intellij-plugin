package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode


interface SettingsTreeChangeNotifier {

    fun notifyTreeRefresh()

    fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {}

    companion object {
        @Topic.AppLevel
        val TREE_REFRESH_TOPIC: Topic<SettingsTreeChangeNotifier> = Topic.create(
            "ConfigCat Settings Tree Refresh",
            SettingsTreeChangeNotifier::class.java
        )
    }
}