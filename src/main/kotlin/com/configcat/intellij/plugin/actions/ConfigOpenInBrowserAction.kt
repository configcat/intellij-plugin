package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ConfigNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class ConfigOpenInBrowserAction : AnAction() {
    companion object {
        const val CONFIGCAT_OPEN_CONFIG_IN_BROWSER_ACTION_ID = "CONFIGCAT_CONFIG_OPEN_IN_BROWSER_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if (selectedNode == null || selectedNode !is ConfigNode) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open in Dashboard action could not be executed without a selected Config Node."
            )
            return
        }

        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigState =
            ConfigCatApplicationConfig.getInstance().state
        val productParent = selectedNode.parent as ProductNode
        val url =
            stateConfig.dashboardBaseUrl + "/" + productParent.product.productId + "/" + selectedNode.config.configId

        BrowserLauncher.instance.open(url)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ProductsConfigsPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        val isEnabled = selectedNode != null && selectedNode is ConfigNode
        e.presentation.isEnabled = isEnabled
        e.presentation.isVisible = true
    }


}