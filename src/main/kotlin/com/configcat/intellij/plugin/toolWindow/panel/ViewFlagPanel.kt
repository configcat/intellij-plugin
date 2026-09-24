package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ConfigCatResponseType
import com.configcat.intellij.plugin.webview.ConfigCatWebViewFailData
import com.configcat.intellij.plugin.webview.ViewType

import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.SimpleToolWindowPanel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class ViewFlagPanel(appData: AppData) : SimpleToolWindowPanel(false, false), Disposable {

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        val webViewPanel = WebViewPanelContainer(appData, ViewType.VIEW_FLAG, {
                returnValue -> processViewFlagResponse(returnValue)
        })
        add(webViewPanel)
    }

    fun processViewFlagResponse(response: String?) {
        val responseData = Constants.decodeConfigCatResponse(response)
        if (responseData == null || responseData.type != ConfigCatResponseType.WEBVIEW_FAIL) {
            notifyInvalidResponse("Invalid response from the server: $response")
            return
        }
        processWebViewFailure(responseData.data)
    }

    private fun notifyInvalidResponse(message: String, e: Throwable? = null) {
        if (e != null) {
            thisLogger().error(message, e)
        } else {
            thisLogger().error(message)
        }
        ConfigCatNotifier.Notify.error("View Flag failed: invalid response from the server.")
    }

    private fun processWebViewFailure(data: JsonElement?) {
        val errorMessage = try {
            if (data == null) {
                "View Flag failed: invalid response from the server."
            } else {
                val failData = Constants.json.decodeFromJsonElement<ConfigCatWebViewFailData>(data)
                if (failData.status == 401) {
                    ConfigCatApplicationConfig.getInstance().state.unAuthenticate()
                    ConfigCatNotifier.Notify.error("Logged out from ConfigCat. Please re-authenticate to continue.")
                }
                "View Flag failed: ${failData.message}"
            }
        } catch (e: SerializationException) {
            thisLogger().error("Failed to parse webview failure response.", e)
            "View Flag failed: invalid response from the server."
        }
        ConfigCatNotifier.Notify.error(errorMessage)
    }

    override fun dispose() {
        // no-op
    }
}

