package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.VIEW_TYPE
import com.configcat.intellij.plugin.webview.WebViewPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import java.awt.EventQueue.invokeLater
import javax.swing.Action
import javax.swing.JComponent


class CreateConfigDialog(val project: Project?, private val product: ProductModel): DialogWrapper(true) {

    init {
        title = "Create Config"
        init()
    }

    override fun createActions(): Array<out Action?> {
        var actions = super.createActions()
        actions = actions.remove(okAction);
        actions = actions.remove(cancelAction);
        return actions
    }

    override fun createCenterPanel(): JComponent {

        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate =
            ConfigCatApplicationConfig.getInstance().state
        val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            product.productId.toString(),
            product.name,
            "",
            "",
            "",
            "",
            ""
        )

        return WebViewPanel(project!!, appData, VIEW_TYPE.CREATE_CONFIG, { returnId -> saveSuccess(returnId) })
    }

    fun saveSuccess(returnId: String?): Unit {
        val productId = product.productId
        try {
            ConfigCatNotifier.Notify.info("Config Successfully created.")
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadConfigs(productId)
        }catch (e:ApiException){
            ErrorHandler.errorNotify(e, "Config create failed. For more information check the logs.", project)
        }

        invokeLater {
            close(OK_EXIT_CODE)
        }
    }


}