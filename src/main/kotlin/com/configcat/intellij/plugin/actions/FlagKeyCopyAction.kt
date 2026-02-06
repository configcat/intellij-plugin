package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.tree.DefaultMutableTreeNode

class FlagKeyCopyAction: AnAction() {
    companion object {
        const val CONFIGCAT_COPY_FLAG_KEY_ACTION_ID = "CONFIGCAT_COPY_FLAG_KEY_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()

        if(selectedElement == null || selectedElement.userObject !is FlagNode) {
            ConfigCatNotifier.Notify.error(e.project,"Copy action could not be executed without a selected Flag Node.")
            return
        }
        val selectedNode =  selectedElement.userObject as FlagNode
        val selection = StringSelection( selectedNode.setting.key)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return clipboard.setContents(selection, selection)
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()

        e.presentation.isEnabled = selectedElement != null &&  selectedElement.userObject is FlagNode
        e.presentation.isVisible = true
    }


}