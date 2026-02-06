package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.messaging.ConnectedConfigChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ConfigNode
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.tree.DefaultMutableTreeNode


class ConfigConnectAction: AnAction() {
    companion object {
        const val CONFIGCAT_CONNECT_ACTION_ID = "CONFIGCAT_CONNECT_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if(selectedNode == null || selectedNode !is ConfigNode) {
            ConfigCatNotifier.Notify.error(e.project, "Create action could not be executed without a selected Config Node.")
            return
        }
        val configCatPropertiesService = ConfigCatPropertiesService.getInstance()
        configCatPropertiesService.setConnectedConfig(selectedNode.config.configId.toString())
        val publisher: ConnectedConfigChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ConnectedConfigChangeNotifier.CONNECTED_CONFIG_CHANGE_TOPIC)
        publisher.notifyConnectedConfigChange()
        e.project?.let {
            val toolWindow =
                ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            val settingsContent = toolWindow?.contentManager?.getContent(1)
            settingsContent?.let { content ->
                toolWindow.contentManager.setSelectedContent(content)
            }

        }

    }

    override fun update(e: AnActionEvent) {
     val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        e.presentation.isEnabled = selectedNode != null && selectedNode is ConfigNode
        e.presentation.isVisible = true
    }

}