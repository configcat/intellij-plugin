package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

class HelpAction : RefreshAction() {
    companion object {
        const val CONFIGCAT_HELP_ACTION_ID = "CONFIGCAT_HELP_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Create a help and feedback panel
        e.project?.let {
            val toolWindow =
                ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            toolWindow?.let { toolWindow ->
                val parentDisposable = Disposer.newDisposable("ConfigCat Help tab")
                Disposer.register(toolWindow.disposable, parentDisposable)
                val myToolWindow = ConfigCatToolWindowFactory.HelpToolWindow(parentDisposable)
                val content = ContentFactory.getInstance().createContent(
                    myToolWindow.getContent(),
                    "Help & Feedback", false
                )
                content.setDisposer(parentDisposable)
                content.isCloseable = true
                toolWindow.contentManager.addContent(content)
                toolWindow.contentManager.setSelectedContent(content)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = true
    }

}
