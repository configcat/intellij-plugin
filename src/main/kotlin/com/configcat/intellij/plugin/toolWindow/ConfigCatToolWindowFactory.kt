package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.toolWindow.panel.HelpPanel
import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.panel.ViewFlagPanel
import com.configcat.intellij.plugin.webview.AppData
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout

class ConfigCatToolWindowFactory : ToolWindowFactory {

    companion object {
        const val CONFIGCAT_TOOL_WINDOW_ID = "ConfigCat"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val productsConfigsPanel = ProductsConfigsTreeToolWindow(project)
        val productsConfigsContent =
            ContentFactory.getInstance().createContent(productsConfigsPanel.getContent(), "Products & Configs", false)
        productsConfigsContent.isCloseable = false
        toolWindow.contentManager.addContent(productsConfigsContent)
        val settingsPanel = FeatureFlagsTreeToolWindow(project)
        val settingsContent =
            ContentFactory.getInstance().createContent(settingsPanel.getContent(), "Feature Flags & Settings", false)
        settingsContent.isCloseable = false
        toolWindow.contentManager.addContent(settingsContent)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ProductsConfigsTreeToolWindow(project: Project) {

        private val configCatPanel = ProductsConfigsPanel.getInstance(project)

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            toolTipText = "Manage products abd configs."
            add(configCatPanel, BorderLayout.CENTER)
        }
    }

    class FeatureFlagsTreeToolWindow(project: Project) {

        private val configCatPanel = SettingsPanel.getInstance(project)

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            toolTipText = "Manage connected config's flags and settings."
            add(configCatPanel, BorderLayout.CENTER)
        }
    }

    class HelpToolWindow() {

        private val helpPanel = HelpPanel()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            toolTipText = "Useful links."
            add(helpPanel, BorderLayout.CENTER)
        }
    }

    class ConfigCatFeatureFlagsViewToolWindow(appData: AppData) {

        private val viewFlagPanel = ViewFlagPanel(appData)
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(viewFlagPanel, BorderLayout.CENTER)
        }
    }

}
