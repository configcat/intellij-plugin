package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler

import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.ConfigNode
import com.configcat.intellij.plugin.toolWindow.FlagNode
import com.configcat.intellij.plugin.toolWindow.ProductNode
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.ApiException

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class OpenFeatureFlagAction : AnAction() {
    companion object {
        const val CONFIGCAT_OPEN_FF_ACTION_ID = "CONFIGCAT_OPEN_FF_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if (selectedNode == null || selectedNode !is FlagNode) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open Feature Flag action could not be executed without a selected Flag Node."
            )
            return
        }
        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate =
            ConfigCatApplicationConfig.getInstance().state

        val configParent = selectedNode.parent as ConfigNode
        val productParent = configParent.parent as ProductNode

        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration),
            stateConfig.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(productParent.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }

        val evaluationVersion = configParent.config.evaluationVersion

        val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            productParent.product.productId.toString(),
            "",
            selectedNode.setting.configId.toString(),
            "",
            "",
            evaluationVersion.toString(),
            selectedNode.setting.settingId.toString()
        )

        EnvironmentSelectDialog(e.project, environments, appData, selectedNode.setting.name ).show()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        val isEnabled = selectedNode != null &&  selectedNode is FlagNode
        e.presentation.isEnabled = isEnabled
        e.presentation.isVisible = true
    }


}