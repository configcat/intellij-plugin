package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.Constants.FEATURE_FLAG_KEY_REGEX
import com.configcat.intellij.plugin.Constants.INPUT_MAX_LENGTH
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.WebViewPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.CreateSettingInitialValues
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.updateSettings.impl.Product
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.remove
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTextField

class CreateFlagDialog(val project: Project?, val config: ConfigModel): DialogWrapper(true) {


    init {
        title = "Create Flag"
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
            config.product.productId.toString(),
            config.product.name,
            config.configId.toString(),
            config.name,
            "",
            "",
            ""
        )

        return WebViewPanel(appData, "createfeatureflag")
    }

    override fun doOKAction() {
        super.doOKAction()

        val configId = config.configId

        //TODO this should be called from the WebView somehow

        try {
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadFlags(configId)
        }catch (e:ApiException){
            ErrorHandler.errorNotify(e, "Flag create failed. For more information check the logs.", project)
        }

    }

    data class SettingTypeDropDown(val name: String, val type: SettingType) : Comparable<SettingTypeDropDown>{

        override fun compareTo(other: SettingTypeDropDown): Int {
            return this.type.compareTo(other.type)
        }

        override fun toString(): String {
            return name
        }
    }
}