package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.webview.AppData
import com.configcat.intellij.plugin.webview.VIEW_TYPE

import com.configcat.intellij.plugin.webview.WebViewPanelContainer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel

class ViewFlagPanel(project: Project, appData: AppData) : SimpleToolWindowPanel(false, false), Disposable {

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        val webViewPanel = WebViewPanelContainer(project, appData, VIEW_TYPE.VIEW_FLAG, null)
        add(webViewPanel)
    }

    override fun dispose() {
    }
}