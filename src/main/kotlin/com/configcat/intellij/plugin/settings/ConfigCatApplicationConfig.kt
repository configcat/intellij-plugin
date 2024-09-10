package com.configcat.intellij.plugin.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


const val DEFAULT_DASHBOARD_BASE_URL = "https://app.configcat.com";
const val DEFAULT_PUBLIC_API_BASE_URL = "https://api.configcat.com";

@State( name = "ConfigCatApplicationConfig", storages = [Storage("configcat-inellij-plugin.xml")])
open class ConfigCatApplicationConfig : PersistentStateComponent<ConfigCatApplicationConfig.ConfigCatApplicationConfigSate> {
    private var appState: ConfigCatApplicationConfigSate = ConfigCatApplicationConfigSate()

    companion object {
        fun getInstance(): ConfigCatApplicationConfig {
            return ApplicationManager.getApplication().getService(ConfigCatApplicationConfig()::class.java)
        }
    }

    override fun getState(): ConfigCatApplicationConfigSate {
        return appState
    }

    override fun loadState(state: ConfigCatApplicationConfigSate) {
        appState = state
    }

    data class ConfigCatApplicationConfigSate(
        override var dashboardBaseUrl: String = DEFAULT_DASHBOARD_BASE_URL,
        override var publicApiBaseUrl: String = DEFAULT_PUBLIC_API_BASE_URL,
    ) : ConfigCatSettings {

        private val key = "configCatKey"
        private val credentialAttributes: CredentialAttributes =
            CredentialAttributes(
                generateServiceName(
                    "configcat-intellij-plugin",
                    key
                )
            )

        override var authConfiguration: String
            get() {
                return PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
            }
            set(value) {
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(credentialAttributes, credentials)
            }


        override fun isConfigured(): Boolean {
              return authConfiguration.isNotEmpty() && dashboardBaseUrl.isNotEmpty() && publicApiBaseUrl.isNotEmpty()
        }

    }

}