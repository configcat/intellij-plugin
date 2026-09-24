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
import com.configcat.publicapi.java.client.model.ConfigModel
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
        val isAuthorized = authConf.basicAuthUserName.isEmpty().not() && authConf.basicAuthPassword.isEmpty().not()


        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            isAuthorized,
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

    fun saveSuccess(jsonString: String?) {
        val responseData = Constants.decodeConfigCatResponse(jsonString)
        if (responseData != null) {
            when (responseData.type) {
                ConfigCatResponseType.FF_CREATE -> {
                    createdFlagId = responseData.data?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                }
                ConfigCatResponseType.WEBVIEW_FAIL -> {
                    val webViewErrorData = parseWebViewFailData(responseData)
                    if (webViewErrorData != null && webViewErrorData.status == 401) {
                        ConfigCatApplicationConfig.getInstance().state.unAuthenticate()
                        ConfigCatNotifier.Notify.error("Logged out from ConfigCat. Please re-authenticate to continue.")
                    } else {
                        ConfigCatNotifier.Notify.error("Flag create failed: ${webViewErrorData?.message ?:
                            "Unknown error"}")
                    }
                    invokeLater { close(CANCEL_EXIT_CODE) }
                    return
                }
                else -> {
                    ConfigCatNotifier.Notify.error("Flag create failed: invalid response from webview.")
                    invokeLater { close(CANCEL_EXIT_CODE) }
                    return
                }
            }
        } else {
            createdFlagId = null
        }

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

    private fun parseWebViewFailData(response: ConfigCatResponseData): ConfigCatWebViewFailData? {
        val responseData = response.data ?: return null
        return try {
            Constants.json.decodeFromJsonElement<ConfigCatWebViewFailData>(responseData)
        } catch (_: SerializationException) {
            null
        }
    }
}
