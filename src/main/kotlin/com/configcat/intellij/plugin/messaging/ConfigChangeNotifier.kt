package com.configcat.intellij.plugin.messaging

import com.intellij.util.messages.Topic

interface ConfigChangeNotifier {


    fun notifyConfigChange()

    companion object {
        @Topic.AppLevel
        val CONFIG_CHANGE_TOPIC: Topic<ConfigChangeNotifier> = Topic.create(
            "ConfigCat Config Changed",
            ConfigChangeNotifier::class.java
        )
    }
}