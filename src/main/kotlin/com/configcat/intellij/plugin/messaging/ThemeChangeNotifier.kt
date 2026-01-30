package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode


interface ThemeChangeNotifier {

    fun notifyThemeChange()

    companion object {
        @Topic.AppLevel
        val THEME_CHANGE_TOPIC: Topic<ThemeChangeNotifier> = Topic.create(
            "Theme Changed",
            ThemeChangeNotifier::class.java
        )
    }
}