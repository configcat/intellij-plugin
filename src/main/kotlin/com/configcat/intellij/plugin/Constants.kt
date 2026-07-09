package com.configcat.intellij.plugin

import com.configcat.intellij.plugin.services.PublicApiConfiguration
import com.configcat.intellij.plugin.webview.ConfigCatResponseData
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object Constants {

    val json =
        Json {
            ignoreUnknownKeys = true
        }

    fun decodePublicApiConfiguration(jsonString: String): PublicApiConfiguration {
        if (jsonString.isEmpty())
            return PublicApiConfiguration("", "")
        return json.decodeFromString(jsonString)
    }

    fun encodePublicApiConfiguration(authConfig: PublicApiConfiguration): String {
        return json.encodeToString(authConfig)
    }

    fun decodeConfigCatResponse(jsonString: String?): ConfigCatResponseData? {
        if (jsonString.isNullOrEmpty()) {
            return null
        }
        return try {
           json.decodeFromString<ConfigCatResponseData>(jsonString)
        } catch (e: SerializationException) {
            thisLogger().error("Failed to parse ConfigCat WebView response data.", e)
            null
        }
    }
}
