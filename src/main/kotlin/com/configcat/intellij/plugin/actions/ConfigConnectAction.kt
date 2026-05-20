package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ConfigNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class ConfigConnectAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_CONNECT_ACTION_ID = "CONFIGCAT_CONNECT_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if (selectedNode == null || selectedNode !is ConfigNode) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Connect action could not be executed without a selected Config Node."
            )
            return
        }
        ConfigConnectionHandler.connectConfig(e.project, selectedNode.config.configId.toString())

    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        updateVisibility(e, selectedNode is ConfigNode)
    }

}
