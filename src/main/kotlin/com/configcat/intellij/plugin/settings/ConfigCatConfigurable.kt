package com.configcat.intellij.plugin.settings

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.dialogs.AuthorizationDialog
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.services.PublicApiConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JTextField


class ConfigCatConfigurable : BoundConfigurable(displayName = "ConfigCat Plugin Settings") {

    private var stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigState =
        ConfigCatApplicationConfig.getInstance().state
    private val dashboardBaseUrlField = JTextField()
    private val publicApiBaseUrlField = JTextField()
    private lateinit var commentAuthenticationGroup: Cell<JEditorPane>
    private val authStatusLabel = JLabel()
    private val authActionButton = JButton()
    private var credentials: PublicApiConfiguration =
        Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

    private val authenticationComment: String =
        "<a href=\"%s/my-account/public-api-credentials\">Get your Basic Auth user name and password</a> " +
            "to access ConfigCat Public API. Note! " +
            "Your ConfigCat account's email address and password will not work here."

    override fun createPanel(): DialogPanel {

        val dialogPanel = panel {
            group("Authentication", true) {
                row {
                    text(
                        "In order to use ConfigCat Feature Flags you have to authorize first " +
                                "with your ConfigCat Public API credentials."
                    )
                }
                row {
                    cell(authStatusLabel)
                    cell(authActionButton).applyToComponent {
                        addActionListener {
                            val dialog = AuthorizationDialog()
                            val success = dialog.showAndGet()
                            if (success) {
                                val authorizationModel = dialog.authorizationModel
                                credentials = if (authorizationModel != null) {
                                    PublicApiConfiguration(authorizationModel.basicAuthUsername,
                                        authorizationModel.basicAuthPassword)
                                } else {
                                    PublicApiConfiguration("", "")
                                }
                                apply()
                            }
                        }
                    }
                }
                row {
                    commentAuthenticationGroup =
                        comment(authenticationComment.format(stateConfig.dashboardBaseUrl))
                }
            }

            group("Plugin Settings", true) {
                row("Dashboard Base URL") {
                    cell(dashboardBaseUrlField)
                        .columns(COLUMNS_MEDIUM)
                        .bindText(stateConfig::dashboardBaseUrl)
                        .validationOnApply(urlValidation())
                        .validationOnInput(urlValidation())

                }
                row {
                    comment(
                        "ConfigCat Dashboard Base URL. " +
                            "Defaults to <a href=\"$DEFAULT_DASHBOARD_BASE_URL\">$DEFAULT_DASHBOARD_BASE_URL</a>."
                    )
                }

                row("Public API Base URL") {
                    cell(publicApiBaseUrlField)
                        .columns(COLUMNS_MEDIUM)
                        .bindText(stateConfig::publicApiBaseUrl)
                        .validationOnInput(urlValidation())
                        .validationOnApply(urlValidation())
                }
                row {
                    comment(
                        "ConfigCat Public Management Base URL. " +
                            "Defaults to <a href=\"$DEFAULT_PUBLIC_API_BASE_URL\">$DEFAULT_PUBLIC_API_BASE_URL</a>."
                    )
                }
            }
        }

        refreshAuthenticationUi()
        return dialogPanel
    }

    private fun refreshAuthenticationUi() {
        val loggedIn = credentials.basicAuthUserName.isNotEmpty()
        authStatusLabel.text = if (loggedIn) "Logged in as ${credentials.basicAuthUserName}"
            else "Login to use ConfigCat Feature Flags"
        authActionButton.text = if (loggedIn) "Unauthorize" else "Authorize"
        authStatusLabel.revalidate()
        authStatusLabel.repaint()
        authActionButton.revalidate()
        authActionButton.repaint()
    }

    private fun urlValidation(): ValidationInfoBuilder.(JTextField) -> ValidationInfo? = {
        if (it.text.isEmpty()) {
            error("The URL cannot be empty.")
        } else {
            null
        }
    }

    override fun isModified(): Boolean {
        val stateAuthConfiguration: PublicApiConfiguration =
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)
        return credentials != stateAuthConfiguration
                || dashboardBaseUrlField.text != stateConfig.dashboardBaseUrl
                || publicApiBaseUrlField.text != stateConfig.publicApiBaseUrl
    }

    override fun apply() {
        if (dashboardBaseUrlField.text.isEmpty()) {
            ConfigCatNotifier.Notify.error("Dashboard Base URL cannot be empty.")
            return
        }

        if (publicApiBaseUrlField.text.isEmpty()) {
            ConfigCatNotifier.Notify.error("Public API Base URL cannot be empty.")
            return
        }
        super.apply()
        stateConfig.authConfiguration = Constants.encodePublicApiConfiguration(credentials)
        stateConfig.dashboardBaseUrl = dashboardBaseUrlField.text
        stateConfig.publicApiBaseUrl = publicApiBaseUrlField.text
        configChangedPublish()
        commentAuthenticationGroup.component.text = authenticationComment.format(stateConfig.dashboardBaseUrl)
        commentAuthenticationGroup.component.updateUI()
        refreshAuthenticationUi()
    }

    private fun configChangedPublish() {
        val publisher: ConfigChangeNotifier =
            ApplicationManager.getApplication().messageBus.syncPublisher(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC)
        publisher.notifyConfigChange()
    }


    override fun reset() {
        val stateAuthConfiguration: PublicApiConfiguration =
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)
        credentials = stateAuthConfiguration
        dashboardBaseUrlField.text = stateConfig.dashboardBaseUrl
        publicApiBaseUrlField.text = stateConfig.publicApiBaseUrl
        refreshAuthenticationUi()
    }
}
