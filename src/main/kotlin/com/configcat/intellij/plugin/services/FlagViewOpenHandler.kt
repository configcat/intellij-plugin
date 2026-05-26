package com.configcat.intellij.plugin.services

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.dialogs.EnvironmentSelectDialog
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

object FlagViewOpenHandler {

    fun openFlagView(
        project: Project?,
        state: ConfigCatApplicationConfig.ConfigCatApplicationConfigState,
        configModel: ConfigModel,
        settingId: Int,
        settingName: String
    ): Boolean {
        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(state.authConfiguration),
            state.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(configModel.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(
                exception,
                "Failed to get environment list. For more information check the logs.",
                project
            )
            return false
        }

        val dialog = EnvironmentSelectDialog(project, environments)
        if (!dialog.showAndGet()) {
            return false
        }
        val selectedEnvironment = dialog.selectedEnvironment

        val authConf = Constants.decodePublicApiConfiguration(state.authConfiguration)
        val appData = AppData(
            state.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            state.dashboardBaseUrl,
            true, //TODO
            configModel.product.productId.toString(),
            "",
            configModel.configId.toString(),
            "",
            selectedEnvironment?.id ?: "",
            configModel.evaluationVersion.toString(),
            settingId.toString()
        )

        val toolWindow = project?.let {
            ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
        } ?: return false

        val featureFlagsViewPanel = ConfigCatToolWindowFactory.ConfigCatFeatureFlagsViewToolWindow(appData)
        val content = ContentFactory.getInstance().createContent(
            featureFlagsViewPanel.getContent(),
            "$settingName (${selectedEnvironment?.name})",
            false
        )
        content.isCloseable = true
        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.setSelectedContent(content)

        return true
    }
}

