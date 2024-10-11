package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.ConfigNode
import com.configcat.intellij.plugin.toolWindow.FlagNode
import com.configcat.intellij.plugin.toolWindow.ProductNode
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.EvaluationVersion

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class OpenInBrowserAction: AnAction() {
    companion object {
        const val CONFIGCAT_OPEN_IN_BROWSER_ACTION_ID = "CONFIGCAT_OPEN_IN_BROWSER_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if(selectedNode == null || (selectedNode !is FlagNode && selectedNode !is ConfigNode)) {
            ConfigCatNotifier.Notify.error(e.project, "Open in Dashboard action could not be executed without a selected Config or Flag Node.")
            return
        }
        var url: String? = null
        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state
        if (selectedNode is ConfigNode) {
            val productParent = selectedNode.parent as ProductNode
            url = stateConfig.dashboardBaseUrl + "/" + productParent.product.productId + "/"+ selectedNode.config.configId
        }
        if (selectedNode is FlagNode) {
            val configParent = selectedNode.parent as ConfigNode
            val productParent = configParent.parent as ProductNode

            val environmentsService = ConfigCatService.createEnvironmentsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
            val environments = environmentsService.getEnvironments(productParent.product.productId)
            val evaluationVersion = configParent.config.evaluationVersion ?: EvaluationVersion.V1
            val orgId = productParent.product.organization?.organizationId
            url = if(evaluationVersion == EvaluationVersion.V1) {
                if(environments.size < 1) {
                    null
                } else {
                    stateConfig.dashboardBaseUrl + '/' + productParent.product.productId + '/' + configParent.config.configId + '/' + environments[0].environmentId + "?settingId=" + selectedNode.setting.settingId
                }
            } else {
                if(environments.size < 1 || orgId == null) {
                    null
                } else {
                    stateConfig.dashboardBaseUrl + "/v2/" + orgId + '/' + productParent.product.productId + '/' + configParent.config.configId + '/' + environments[0].environmentId + '/' + selectedNode.setting.settingId
                }
            }

        }
        if(url == null) {
            ConfigCatNotifier.Notify.error(e.project, "Open in Dashboard action could not be executed. Missing information to create a valid URL.")
            return
        }
        BrowserLauncher.instance.open(url)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        val isEnabled  = selectedNode != null && (selectedNode is ConfigNode || selectedNode is FlagNode)
        e.presentation.isEnabled = isEnabled
        e.presentation.isVisible = true
    }


}