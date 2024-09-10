package com.configcat.intellij.plugin

import com.configcat.intellij.plugin.services.PublicApiConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object Constants {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    fun decodePublicApiConfiguration(jsonString: String):  PublicApiConfiguration {
        if(jsonString.isEmpty())
            return  PublicApiConfiguration("", "")
        return json.decodeFromString(jsonString)
    }

    fun encodePublicApiConfiguration(authConfig: PublicApiConfiguration): String {
        return json.encodeToString(authConfig)
    }
}