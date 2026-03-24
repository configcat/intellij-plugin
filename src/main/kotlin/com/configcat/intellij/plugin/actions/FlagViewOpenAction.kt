package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler

import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.ApiException

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class FlagViewOpenAction : AnAction() {
    companion object {
        const val CONFIGCAT_OPEN_FF_ACTION_ID = "CONFIGCAT_OPEN_FF_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val selectedNode = selectedElement?.userObject
        if ((selectedNode == null || selectedNode !is FlagNode) || configModel == null) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open Feature Flag action could not be executed without a selected Flag Node or a connected Config."
            )
            return
        }
        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate =
            ConfigCatApplicationConfig.getInstance().state

        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration),
            stateConfig.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(configModel.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }

        val evaluationVersion = configModel.evaluationVersion

        val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            configModel.product.productId.toString(),
            "",
            selectedNode.setting.configId.toString(),
            "",
            "",
            evaluationVersion.toString(),
            selectedNode.setting.settingId.toString()
        )

        EnvironmentSelectDialog(e.project, environments, appData, selectedNode.setting.name).show()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val selectedNode = selectedElement?.userObject
        val isEnabled = selectedNode != null && selectedNode is FlagNode && configModel != null
        e.presentation.isEnabled = isEnabled
        e.presentation.isVisible = true
    }


}