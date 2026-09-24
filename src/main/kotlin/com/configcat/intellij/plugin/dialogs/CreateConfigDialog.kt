package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ConfigCatResponseData
import com.configcat.intellij.plugin.webview.ConfigCatResponseType
import com.configcat.intellij.plugin.webview.ConfigCatWebViewFailData
import com.configcat.intellij.plugin.webview.ViewType
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.awt.EventQueue.invokeLater
import javax.swing.Action
import javax.swing.JComponent


class CreateConfigDialog(val project: Project?, private val product: ProductModel) : DialogWrapper(true) {

    var createdConfigId: String? = null
        private set

    init {
        title = "Create Config"
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
        val isAuthorized = authConf.basicAuthUserName.isEmpty().not() && authConf.basicAuthPassword.isEmpty().not()

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            isAuthorized,
            product.productId.toString(),
            product.name,
            "",
            "",
            "",
            "",
            ""
        )

        return WebViewPanelContainer(appData, ViewType.CREATE_CONFIG) { returnId -> saveSuccess(returnId) }
    }

    fun saveSuccess(jsonString: String?) {
        val productId = product.productId
        val responseData = Constants.decodeConfigCatResponse(jsonString)
        if (responseData != null) {
            when (responseData.type) {
                ConfigCatResponseType.CONFIG_CREATE -> {
                    createdConfigId = responseData.data?.jsonPrimitive?.contentOrNull?.ifBlank { null }
                }
                ConfigCatResponseType.WEBVIEW_FAIL -> {
                    val webViewErrorData = parseWebViewFailData(responseData)
                    if (webViewErrorData != null && webViewErrorData.status == 401) {
                        ConfigCatApplicationConfig.getInstance().state.unAuthenticate()
                        ConfigCatNotifier.Notify.error("Logged out from ConfigCat. Please re-authenticate to continue.")
                    } else {
                        ConfigCatNotifier.Notify.error("Config create failed: ${webViewErrorData?.message ?:
                            "Unknown error"}")
                    }
                    invokeLater { close(CANCEL_EXIT_CODE) }
                    return
                }
                else -> {
                    ConfigCatNotifier.Notify.error("Config create failed: invalid response from webview.")
                    invokeLater { close(CANCEL_EXIT_CODE) }
                    return
                }
            }
        } else {
            createdConfigId = null
        }

        try {
            ConfigCatNotifier.Notify.info("Config Successfully created.")
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadConfigs(productId)
        } catch (e: ApiException) {
            ErrorHandler.errorNotify(e, "Config create failed. For more information check the logs.", project)
        }

        invokeLater {
            close(OK_EXIT_CODE)
        }
    }

    private fun parseWebViewFailData(response: ConfigCatResponseData): ConfigCatWebViewFailData? {
        val responseData = response.data ?: return null
        return try {
            Constants.json.decodeFromJsonElement<ConfigCatWebViewFailData>(responseData)
        } catch (_: SerializationException) {
            null
        }
    }


}
