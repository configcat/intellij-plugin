package com.configcat.intellij.plugin.toolWindow


import com.configcat.intellij.plugin.webview.AppData

import com.configcat.intellij.plugin.webview.WebViewPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel




class ViewFlagPanel(appData: AppData) : SimpleToolWindowPanel(false, false), Disposable {

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        add(WebViewPanel(appData,  "featureflagsetting"))
    }

    override fun dispose() {
    }
}