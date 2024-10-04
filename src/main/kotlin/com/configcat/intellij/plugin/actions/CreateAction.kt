package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.dialogs.CreateConfigDialog
import com.configcat.intellij.plugin.dialogs.CreateFlagDialog
import com.configcat.intellij.plugin.messaging.TreeChangeNotifier
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.ConfigNode
import com.configcat.intellij.plugin.toolWindow.ProductNode
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import javax.swing.tree.DefaultMutableTreeNode


class CreateAction: AnAction() {
    companion object {
        const val CONFIGCAT_CREATE_ACTION_ID = "CONFIGCAT_CREATE_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.getData(ConfigCatPanel.CONFIGCAT_TREE_SELECTED_NODE_DATA_KEY)
        val selectedNode = selectedElement?.userObject
        if(selectedNode == null || (selectedNode !is ProductNode && selectedNode !is ConfigNode)) {
            ConfigCatNotifier.Notify.error(e.project, "Create action could not be executed without a selected Product or Config Node.")
            return
        }
        if (selectedNode is ProductNode) {
            CreateConfigDialog(e.project, selectedNode.product).showAndGet()
            nodeRefreshPublish(selectedElement)
        }
        if (selectedNode is ConfigNode) {
            CreateFlagDialog(e.project, selectedNode.config).showAndGet()
            nodeRefreshPublish(selectedElement)
        }

    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.getData(ConfigCatPanel.CONFIGCAT_TREE_SELECTED_NODE_DATA_KEY)
        val selectedNode = selectedElement?.userObject
        e.presentation.isEnabled = selectedNode != null && (selectedNode is ConfigNode || selectedNode is ProductNode)
        e.presentation.isVisible = true
    }

    private fun nodeRefreshPublish(node: DefaultMutableTreeNode) {
        val publisher: TreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            TreeChangeNotifier.TREE_CHANGE_TOPIC)
        publisher.notifyTreeNodeRefresh(node)
    }

}