package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ViewType
import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.rd.util.remove
import javax.swing.Action
import javax.swing.JComponent

class AuthorizationDialog : DialogWrapper(true) {

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

        return WebViewPanelContainer(appData, ViewType.AUTHORIZATION) {
            close(OK_EXIT_CODE)
        }
    }
}
