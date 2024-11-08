package com.configcat.intellij.plugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout


class ConfigCatToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = ConfigCatToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ConfigCatToolWindow(project: Project, toolWindow: ToolWindow) {

        init {
            toolWindow.title = "ConfigCat Feature Flags"
        }

        private val helpPanel = HelpPanel()
        private val configCatPanel = ConfigCatPanel.getInstance(project)

        fun getContent() = JBPanel<JBPanel<*>>().apply {

            layout = BorderLayout()
            val tabsPane = JBTabbedPane()
            tabsPane.insertTab("Feature Flags & Settings", null, configCatPanel, "Manage products, configs and flags & settings.", 0)
            tabsPane.insertTab("Help & Feedback", null, helpPanel, "Useful links.", 1)

            add(tabsPane, BorderLayout.CENTER)
        }
    }
}
