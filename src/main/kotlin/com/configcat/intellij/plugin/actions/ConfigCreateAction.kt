package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.dialogs.CreateConfigDialog
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class ConfigCreateAction: AnAction() {
    companion object {
        const val CONFIGCAT_CONFIG_CREATE_ACTION_ID = "CONFIGCAT_CONFIG_CREATE_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if(selectedNode == null || selectedNode !is ProductNode) {
            ConfigCatNotifier.Notify.error(e.project, "Create Config action could not be executed without a selected Product.")
            return
        }
        CreateConfigDialog(e.project, selectedNode.product).showAndGet()
        nodeRefreshPublish(selectedElement)
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()
        val selectedNode = selectedElement?.userObject
        e.presentation.isEnabled = selectedNode != null &&  selectedNode is ProductNode
        e.presentation.isVisible = true
    }

    private fun nodeRefreshPublish(node: DefaultMutableTreeNode) {
        val publisher: ProductsConfigsTreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC)
        publisher.notifyTreeNodeRefresh(node)
    }

}