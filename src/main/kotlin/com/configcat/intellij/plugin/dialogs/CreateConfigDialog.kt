package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.Constants.INPUT_MAX_LENGTH
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.WebViewPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.CreateConfigRequest
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.collaboration.ui.util.name
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.remove
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTextField

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

        return WebViewPanel(project!!, appData, "createconfig")
    }

    override fun doOKAction() {

        val productId = product.productId

//TODO this should be called from the webview

        try {
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadConfigs(productId)
        }catch (e:ApiException){
            ErrorHandler.errorNotify(e, "Config create failed. For more information check the logs.", project)
        }
        super.doOKAction()
    }

}