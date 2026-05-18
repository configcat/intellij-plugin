package com.configcat.intellij.plugin.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


const val DEFAULT_DASHBOARD_BASE_URL = "https://app.configcat.com"
const val DEFAULT_PUBLIC_API_BASE_URL = "https://api.configcat.com"
const val EMPTY_CREDENTIALS = "{\"basicAuthUserName\":\"\",\"basicAuthPassword\":\"\"}"

@State(name = "ConfigCatApplicationConfig", storages = [Storage("configcat-intellij-plugin.xml")])
open class ConfigCatApplicationConfig :
    PersistentStateComponent<ConfigCatApplicationConfig.ConfigCatApplicationConfigState> {
    private var appState: ConfigCatApplicationConfigState = ConfigCatApplicationConfigState()

    companion object {
        fun getInstance(): ConfigCatApplicationConfig {
            return ApplicationManager.getApplication().getService(ConfigCatApplicationConfig()::class.java)
        }
    }

    override fun getState(): ConfigCatApplicationConfigState {
        return appState
    }

    override fun loadState(state: ConfigCatApplicationConfigState) {
        appState = state
    }

    data class ConfigCatApplicationConfigState(
        override var dashboardBaseUrl: String = DEFAULT_DASHBOARD_BASE_URL,
        override var publicApiBaseUrl: String = DEFAULT_PUBLIC_API_BASE_URL,
    ) : ConfigCatSettings {

        private val key = "configCatKey"
        private val credentialAttributes: CredentialAttributes = run {
            val serviceName = generateServiceName("configcat-intellij-plugin", key)
            val clazz = CredentialAttributes::class.java
            // 261+ uses (String, String, Boolean, Boolean); 241 uses (String, String, Class, Boolean, Boolean).
            // Use reflection to pick the right constructor and avoid deprecated-API bytecode references.
            try {
                clazz.getConstructor(
                    String::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                    .newInstance(serviceName, key, false, false)
            } catch (_: NoSuchMethodException) {
                clazz.getConstructor(
                    String::class.java,
                    String::class.java,
                    Class::class.java,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                    .newInstance(serviceName, key, null, false, false)
            }
        }

        override var authConfiguration: String
            get() {
                return PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
            }
            set(value) {
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(credentialAttributes, credentials)
            }


        override fun isConfigured(): Boolean {
            return authConfiguration.isNotEmpty() &&
                authConfiguration != EMPTY_CREDENTIALS &&
                dashboardBaseUrl.isNotEmpty() &&
                publicApiBaseUrl.isNotEmpty()
        }

    }

}

