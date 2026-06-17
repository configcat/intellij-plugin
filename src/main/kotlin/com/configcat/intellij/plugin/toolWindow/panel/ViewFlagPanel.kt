package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.ViewType

import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel

class ViewFlagPanel(appData: AppData) : SimpleToolWindowPanel(false, false), Disposable {

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        val webViewPanel = WebViewPanelContainer(appData, ViewType.VIEW_FLAG, null)
        add(webViewPanel)
    }

    override fun dispose() {
        // no-op
    }
}

