package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler

import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog
import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog.EnvironmentDropDown
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.ApiException

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import javax.swing.tree.DefaultMutableTreeNode


class FlagViewOpenAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_OPEN_FF_ACTION_ID = "CONFIGCAT_OPEN_FF_ACTION_ID"
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
        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(state.authConfiguration),
            state.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(configModel.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }
        val selectedEnvironment: EnvironmentDropDown?
        val dialog = EnvironmentSelectDialog(e.project, environments, )
        if (dialog.showAndGet()) {
            selectedEnvironment = dialog.selectedEnvironment
            // use envId
        } else {
            return
        }

        val evaluationVersion = configModel.evaluationVersion

        val authConf = Constants.decodePublicApiConfiguration(state.authConfiguration)

        val appData = AppData(
            state.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            state.dashboardBaseUrl,
            configModel.product.productId.toString(),
            "",
            selectedNode.setting.configId.toString(),
            "",
            selectedEnvironment?.id ?: "",
            evaluationVersion.toString(),
            selectedNode.setting.settingId.toString()
        )

        e.project?.let { project ->
            val toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            toolWindow?.let { toolWindow ->
                val featureFlagsViewPanel =
                    ConfigCatToolWindowFactory.ConfigCatFeatureFlagsViewToolWindow(appData)
                val content = ContentFactory.getInstance().createContent(
                    featureFlagsViewPanel.getContent(),
                    "${selectedNode.setting.name} (${selectedEnvironment?.name})", false
                )
                content.isCloseable = true
                toolWindow.contentManager.addContent(content)
                toolWindow.contentManager.setSelectedContent(content)
            }

        }
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val isEnabled = selectedElement?.userObject is FlagNode && configModel != null
        updateVisibility(e, isEnabled)
    }


}