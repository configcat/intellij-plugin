package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ConfigCatResponseData
import com.configcat.intellij.plugin.webview.ConfigCatResponseType
import com.configcat.intellij.plugin.webview.ConfigCatWebViewFailData
import com.configcat.intellij.plugin.webview.ViewType
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.awt.EventQueue.invokeLater
import javax.swing.Action
import javax.swing.JComponent

class AuthorizationDialog : DialogWrapper(true) {

    var authorizationModel: AuthorizationModel? = null
        private set

    init {
        title = "Authorization"
        init()
    }

    override fun createActions(): Array<out Action?> {
        var actions = super.createActions()
        actions = actions.remove(okAction)
        actions = actions.remove(cancelAction)
        return actions
    }

    override fun createCenterPanel(): JComponent {
        val stateConfig = ConfigCatApplicationConfig.getInstance().state
        val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)
        val isAuthorized = authConf.basicAuthUserName.isEmpty().not() && authConf.basicAuthPassword.isEmpty().not()

        val appData = AppData(
            stateConfig.publicApiBaseUrl,
            authConf.basicAuthUserName,
            authConf.basicAuthPassword,
            stateConfig.dashboardBaseUrl,
            isAuthorized,
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )

        return WebViewPanelContainer(appData, ViewType.AUTHORIZATION, ) {
            returnValue -> processAuthorizationResponse(returnValue)
        }
    }

    fun processAuthorizationResponse(response: String?) {
        val responseData = Constants.decodeConfigCatResponse(response)
        if (responseData == null) {
            notifyInvalidResponse("Invalid response from the server: $response")
            return
        }
        if (!processResponseData(responseData)) {
            return
        }
        invokeLater {
            close(OK_EXIT_CODE)
        }
    }

    private fun processResponseData(response: ConfigCatResponseData): Boolean {
        return when (response.type) {
            ConfigCatResponseType.AUTHORIZATION -> processAuthorizationData(response.data)
            ConfigCatResponseType.WEBVIEW_FAIL -> {
                processWebViewFailure(response.data)
                false
            }
            else -> {
                notifyInvalidResponse("Unexpected authorization response type: ${response.type}")
                false
            }
        }
    }

    private fun processAuthorizationData(data: JsonElement?): Boolean {
        if (data == null) {
            notifyInvalidResponse("Missing authorization response data.")
            return false
        }

        if (data is JsonPrimitive && data.contentOrNull == "unauthorize") {
            ConfigCatNotifier.Notify.info("Logged out from ConfigCat.")
            authorizationModel = null
            return true
        }

        return try {
            authorizationModel = Constants.json.decodeFromJsonElement<AuthorizationModel>(data)
            ConfigCatNotifier.Notify.info("Logged in to ConfigCat. Email: ${authorizationModel?.email}")
            thisLogger().info("Authorization successful for user: ${authorizationModel?.basicAuthUsername}")
            true
        } catch (e: SerializationException) {
            notifyInvalidResponse("Failed to parse authorization response.", e)
            false
        }
    }

    private fun processWebViewFailure(data: JsonElement?) {
        val errorMessage = try {
            if (data == null) {
                "Authorization failed: invalid response from the server."
            } else {
                val failData = Constants.json.decodeFromJsonElement<ConfigCatWebViewFailData>(data)
                if (failData.status == 401) {
                    ConfigCatApplicationConfig.getInstance().state.unAuthenticate()
                }
                "Authorization failed: ${failData.message}"
            }
        } catch (e: SerializationException) {
            thisLogger().error("Failed to parse webview failure response.", e)
            "Authorization failed: invalid response from the server."
        }

        ConfigCatNotifier.Notify.error(errorMessage)
        invokeLater { close(CANCEL_EXIT_CODE) }
    }

    private fun notifyInvalidResponse(message: String, e: Throwable? = null) {
        if (e != null) {
            thisLogger().error(message, e)
        } else {
            thisLogger().error(message)
        }
        ConfigCatNotifier.Notify.error("Authorization failed: invalid response from the server.")
        invokeLater { close(CANCEL_EXIT_CODE) }
    }

    @Serializable
    data class AuthorizationModel(
        val basicAuthUsername: String,
        val basicAuthPassword: String,
        val email: String,
        val fullName: String,
    )
}
