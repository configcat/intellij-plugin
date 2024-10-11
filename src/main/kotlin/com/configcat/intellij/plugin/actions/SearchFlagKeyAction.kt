package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.FlagNode
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class SearchFlagKeyAction: AnAction() {
    companion object {
        const val CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID = "CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode =  selectedElement?.userObject
        if(selectedNode == null || selectedNode !is FlagNode ) {
            ConfigCatNotifier.Notify.error(e.project, "Search Flag key action could not be executed without a selected Flag Node.")
            return
        }
        val settingKey = selectedNode.setting.key
        if(settingKey == null) {
            ConfigCatNotifier.Notify.error(e.project, "Search Flag key action could not be executed. Missing Setting Key.")
            return
        }
        val findModel = FindModel()

        findModel.stringToFind = settingKey
        findModel.isCaseSensitive = true
        findModel.isFindAll = true
        FindManager.getInstance(e.project).showFindDialog(findModel,{})
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        e.presentation.isEnabled = selectedElement != null &&  selectedElement.userObject is FlagNode
        e.presentation.isVisible = true
    }


}