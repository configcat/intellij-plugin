package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.PublicApiConfiguration
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ViewType
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import kotlinx.serialization.Serializable
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
        if(response?.isNotEmpty() == true) {
            if( response == "unauthorize") {
                ConfigCatNotifier.Notify.info("Logged out from ConfigCat.")
                authorizationModel = null
            } else {
                authorizationModel = Constants.json.decodeFromString(response)
                val newAuthConfig = PublicApiConfiguration(
                    authorizationModel?.basicAuthUsername ?: "" ,
                    authorizationModel?.basicAuthPassword ?: ""
                )
                ConfigCatNotifier.Notify.info("Logged in to ConfigCat. Email: ${authorizationModel?.email}")
                thisLogger().info("Authorization successful for user: $newAuthConfig")
            }
        }

        invokeLater {
            close(OK_EXIT_CODE)
        }
    }

    @Serializable
    data class AuthorizationModel(
        val basicAuthUsername: String,
        val basicAuthPassword: String,
        val email: String,
        val fullName: String,
    )
}
