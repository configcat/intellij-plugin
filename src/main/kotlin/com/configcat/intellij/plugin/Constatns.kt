package com.configcat.intellij.plugin

import com.configcat.intellij.plugin.services.PublicApiConfiguration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object Constants {

    const val FEATURE_FLAG_KEY_REGEX = "^[a-zA-Z]+[a-zA-Z0-9_-]*$"
    const val INPUT_MAX_LENGTH = 255

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