package com.configcat.intellij.plugin.webview

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

object ConfigCatResponseType {
    const val NONE = "none"
    const val AUTHORIZATION = "authorization"
    const val CONFIG_CREATE = "config-create"
    const val FF_CREATE = "ff-create"
    const val WEBVIEW_FAIL = "webview-fail"
}

@Serializable
data class ConfigCatResponseData(
    val type: String = ConfigCatResponseType.NONE,
    val data: JsonElement? = null,
)

@Serializable
data class ConfigCatWebViewFailData(
    val message: String,
    val status: Int? = null,
)
