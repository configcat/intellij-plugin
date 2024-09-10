package com.configcat.intellij.plugin.settings

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.services.PublicApiConfiguration
import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPasswordField
import javax.swing.JTextField


class ConfigCatConfigurable: Configurable {

    private var stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state
    private val authUserNameField = JTextField()
    private val authPasswordField = JPasswordField()
    private val dashboardBaseUrlField = JTextField()
    private val publicApiBaseUrlField = JTextField()

    override fun createComponent(): DialogPanel {
        return panel {
            group("Authentication" , true)  {
                row {
                    text("In order to use ConfigCat Feature Flags you have to authorize first with your ConfigCat Public API credentials.")
                }
                row("Basic auth user name") {
                    cell(authUserNameField)
                        .columns(COLUMNS_MEDIUM)
                }
                row("Basic auth password") {
                    cell(authPasswordField)
                        .columns(COLUMNS_MEDIUM)
                }

                row {
                    comment("<a href=\"${stateConfig.dashboardBaseUrl}/my-account/public-api-credentials\">Get your Basic Auth user name and password</a> to access ConfigCat Public API. Note! Your ConfigCat account's email address and password will not work here.")
                }
            }
            group("Plugin Settings" , true) {
                row("Dashboard base url") {
                    cell(dashboardBaseUrlField)
                        .columns(COLUMNS_MEDIUM)
                }
                row {
                    comment("ConfigCat Dashboard Base URL. Defaults to <a href=\"$DEFAULT_DASHBOARD_BASE_URL\">$DEFAULT_DASHBOARD_BASE_URL</a>.")
                }

                row("Public API base url") {
                    cell(publicApiBaseUrlField)
                        .columns(COLUMNS_MEDIUM)
                }
                row {
                    comment("ConfigCat Public Management Base URL. Defaults to <a href=\"$DEFAULT_PUBLIC_API_BASE_URL\">$DEFAULT_PUBLIC_API_BASE_URL</a>.")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val credentials = PublicApiConfiguration(authUserNameField.text, String(authPasswordField.password))
        val stateAuthConfiguration: PublicApiConfiguration = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)
        return credentials != stateAuthConfiguration
                || dashboardBaseUrlField.text != stateConfig.dashboardBaseUrl
                || publicApiBaseUrlField.text != stateConfig.publicApiBaseUrl
    }

    override fun apply() {
        if(dashboardBaseUrlField.text.isEmpty()){
            ConfigCatNotifier.Notify.error("Dashboard base url cannot be empty.")
            return
        }

        if(publicApiBaseUrlField.text.isEmpty()){
            ConfigCatNotifier.Notify.error("Public API base url cannot be empty.")
            return
        }

        val credentials = PublicApiConfiguration(authUserNameField.text, String(authPasswordField.password))

        val meService = ConfigCatService.createMeService(credentials, publicApiBaseUrlField.text)
        try {
            val me = meService.me
            ConfigCatNotifier.Notify.info("Logged in to ConfigCat. Email: ${me.email}")
        } catch (exception: ApiException) {
            ConfigCatNotifier.Notify.error("Authentication failed.")
            thisLogger().error("ConfigCat authorization failed.", exception)
            return
        }

        stateConfig.authConfiguration = Constants.encodePublicApiConfiguration(credentials)
        stateConfig.dashboardBaseUrl = dashboardBaseUrlField.text
        stateConfig.publicApiBaseUrl = publicApiBaseUrlField.text
        configChangedPublish()
    }

    private fun configChangedPublish() {
        val publisher: ConfigChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC)
        publisher.notifyConfigChange()
    }

    override fun getDisplayName(): String {
        return "Config Cat Plugin Settings"
    }

    override fun reset() {
        val stateAuthConfiguration: PublicApiConfiguration = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)

        authUserNameField.text = stateAuthConfiguration.basicAuthUserName
        authPasswordField.text = stateAuthConfiguration.basicAuthPassword
        dashboardBaseUrlField.text = stateConfig.dashboardBaseUrl
        publicApiBaseUrlField.text = stateConfig.publicApiBaseUrl
    }
}

