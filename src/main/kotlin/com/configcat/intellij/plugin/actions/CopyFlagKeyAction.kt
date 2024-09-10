package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.FlagNode
import com.intellij.openapi.actionSystem.*
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.tree.DefaultMutableTreeNode


class CopyFlagKeyAction: AnAction() {
    companion object {
        const val CONFIGCAT_COPY_FLAG_KEY_ACTION_ID = "CONFIGCAT_COPY_FLAG_KEY_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.getData(ConfigCatPanel.CONFIGCAT_TREE_SELECTED_NODE_DATA_KEY)
        if(selectedElement == null || selectedElement.userObject !is FlagNode) {
            ConfigCatNotifier.Notify.error("Copy action could not be executed without a selected Flag Node.")
            return
        }
        val selectedNode =  selectedElement.userObject as FlagNode
        val selection = StringSelection( selectedNode.setting.key)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return clipboard.setContents(selection, selection)
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.getData(ConfigCatPanel.CONFIGCAT_TREE_SELECTED_NODE_DATA_KEY)
        e.presentation.isEnabled = selectedElement != null &&  selectedElement.userObject is FlagNode
        e.presentation.isVisible = true
    }


}