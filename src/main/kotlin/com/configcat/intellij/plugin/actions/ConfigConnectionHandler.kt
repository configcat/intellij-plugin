package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.messaging.ConnectedConfigChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

object ConfigConnectionHandler {

    fun connectConfig(project: Project?, configId: String) {
        val configCatPropertiesService = ConfigCatPropertiesService.getInstance()
        configCatPropertiesService.setConnectedConfig(configId)

        val publisher: ConnectedConfigChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ConnectedConfigChangeNotifier.CONNECTED_CONFIG_CHANGE_TOPIC
        )
        publisher.notifyConnectedConfigChange()

        project?.let {
            val toolWindow =
                ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            val settingsContent = toolWindow?.contentManager?.getContent(1)
            settingsContent?.let { content ->
                toolWindow.contentManager.setSelectedContent(content)
            }
        }
    }
}

