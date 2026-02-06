package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic

interface ConnectedConfigChangeNotifier {


    fun notifyConnectedConfigChange()

    companion object {
        @Topic.AppLevel
        val CONNECTED_CONFIG_CHANGE_TOPIC: Topic<ConnectedConfigChangeNotifier> = Topic.create(
            "ConfigCat Connected Config Changed",
            ConnectedConfigChangeNotifier::class.java
        )
    }
}