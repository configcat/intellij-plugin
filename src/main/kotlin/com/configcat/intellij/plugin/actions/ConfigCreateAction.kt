package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.dialogs.CreateConfigDialog
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import javax.swing.tree.DefaultMutableTreeNode


class ConfigCreateAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_CONFIG_CREATE_ACTION_ID = "CONFIGCAT_CONFIG_CREATE_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if (selectedNode == null || selectedNode !is ProductNode) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Create Config action could not be executed without a selected Product."
            )
            return
        }

        val dialog = CreateConfigDialog(e.project, selectedNode.product)
        val isSuccessful = dialog.showAndGet()
        val configIdToSelect = if (isSuccessful) {
            autoConnectCreatedConfig(e.project,  dialog.createdConfigId)
            dialog.createdConfigId
        } else null

        nodeRefreshPublish(selectedElement, configIdToSelect)
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()
        val selectedNode = selectedElement?.userObject
        updateVisibility(e, selectedNode is ProductNode)
    }

    private fun nodeRefreshPublish(node: DefaultMutableTreeNode, configIdToSelect: String?) {
        val publisher: ProductsConfigsTreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC
        )
        publisher.notifyTreeNodeRefresh(node, configIdToSelect)
    }

    private fun autoConnectCreatedConfig(project: Project?, createdConfigId: String?) {
        if (createdConfigId.isNullOrBlank()) {
            thisLogger().error("Config was created successfully but returned config ID is missing. Skipping auto-connect.")
            return
        }
        ConfigConnectionHandler.connectConfig(project, createdConfigId)
    }

}
