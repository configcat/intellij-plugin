package com.configcat.intellij.plugin.toolWindow

import com.configcat.publicapi.java.client.model.SettingModel
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

    //TODO refactor classes and folders?
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val treeToolWindow = FeatureFlagsTreeToolWindow(project, toolWindow)
        val treeContent = ContentFactory.getInstance().createContent(treeToolWindow.getContent(), "Feature Flags & Settings", false)
        treeContent.isCloseable = false
        toolWindow.contentManager.addContent(treeContent)

        val helpToolWindow = HelpToolWindow(project, toolWindow)
        val helpContent = ContentFactory.getInstance().createContent(helpToolWindow.getContent(), "Help & Feedback", false)
        helpContent.isCloseable = false
        toolWindow.contentManager.addContent(helpContent)
    }

    override fun shouldBeAvailable(project: Project) = true

    class FeatureFlagsTreeToolWindow(project: Project, toolWindow: ToolWindow) {

        private val configCatPanel = ConfigCatPanel.getInstance(project)

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            toolTipText = "Manage products, configs and flags & settings."
            add(configCatPanel, BorderLayout.CENTER)
        }
    }

    class HelpToolWindow(project: Project, toolWindow: ToolWindow) {

        private val helpPanel = HelpPanel()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            toolTipText = "Useful links."
            add(helpPanel, BorderLayout.CENTER)
        }
    }

    class ConfigCatFeatureFlagsViewToolWindow(project: Project, toolWindow: ToolWindow, appData: AppData) {

        private val viewFlagPanel = ViewFlagPanel(appData)
        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(viewFlagPanel, BorderLayout.CENTER)
        }
    }

}
