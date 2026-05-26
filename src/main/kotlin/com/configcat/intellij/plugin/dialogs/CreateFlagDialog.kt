package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ViewType
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import java.awt.EventQueue.invokeLater
import javax.swing.Action
import javax.swing.JComponent

class CreateFlagDialog(val project: Project?, val config: ConfigModel) : DialogWrapper(true) {

    var createdFlagId: Int? = null
        private set

    init {
        title = "Create Flag"
        init()
    }

    override fun createActions(): Array<out Action?> {
        var actions = super.createActions()
        actions = actions.remove(okAction)
        actions = actions.remove(cancelAction)
        return actions
    }

    override fun createCenterPanel(): JComponent {

        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigState =
            ConfigCatApplicationConfig.getInstance().state
        val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            stateConfig.isConfigured(), //TODO maybe just pass true
            config.product.productId.toString(),
            config.product.name,
            config.configId.toString(),
            config.name,
            "",
            "",
            ""
        )
        return WebViewPanelContainer(appData, ViewType.CREATE_FLAG, { returnId -> saveSuccess(returnId) })
    }

    fun saveSuccess(returnId: String?) {
        createdFlagId = returnId?.toIntOrNull()
        val configId = config.configId
        try {
            ConfigCatNotifier.Notify.info("Feature Flag Successfully created.")
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadFlags(configId)
        } catch (e: ApiException) {
            ErrorHandler.errorNotify(e, "Flag create failed. For more information check the logs.", project)
        }

        invokeLater {
            close(OK_EXIT_CODE)
        }
    }
}

