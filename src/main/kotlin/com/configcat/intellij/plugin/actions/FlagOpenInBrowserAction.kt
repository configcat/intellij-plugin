package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.EvaluationVersion

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class FlagOpenInBrowserAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_FLAG_OPEN_CONFIG_IN_BROWSER_ACTION_ID = "CONFIGCAT_FLAG_OPEN_IN_BROWSER_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val selectedNode = selectedElement?.userObject
        if ((selectedNode == null || selectedNode !is FlagNode) || configModel == null) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open in Dashboard action could not be executed without a connected Config or selected Flag Node."
            )
            return
        }
        val productModel = configModel.product

        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(state.authConfiguration),
            state.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(configModel.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception, "Failed to get environment list. For more information check the logs.", e.project)
            return
        }
        val evaluationVersion = configModel.evaluationVersion
        val orgId = productModel.organization.organizationId
        val url = if (!environments.isEmpty()) {
            if (evaluationVersion == EvaluationVersion.V1) {
                state.dashboardBaseUrl + '/' + productModel.productId + '/' + configModel.configId + '/' + environments[0].environmentId + "?settingId=" + selectedNode.setting.settingId
            } else {
                state.dashboardBaseUrl + "/v2/" + orgId + '/' + productModel.productId + '/' + configModel.configId + '/' + environments[0].environmentId + '/' + selectedNode.setting.settingId
            }
        } else {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open in Dashboard action could not be executed. Missing information to create a valid URL."
            )
            return
        }

        BrowserLauncher.instance.open(url)
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val isEnabled = selectedElement?.userObject is FlagNode && configModel != null
        updateVisibility(e, isEnabled)
    }


}