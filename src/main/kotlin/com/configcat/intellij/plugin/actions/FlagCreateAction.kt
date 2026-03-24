package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.dialogs.CreateFlagDialog
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service


class FlagCreateAction : AnAction() {
    companion object {
        const val CONFIGCAT_FLAG_CREATE_ACTION_ID = "CONFIGCAT_FLAG_CREATE_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        if (configModel == null) {
            ConfigCatNotifier.Notify.error(e.project, "Create action could not be executed without a connected Config.")
            return
        }

        CreateFlagDialog(e.project, configModel).showAndGet()
        nodeRefreshPublish()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = true
    }

    private fun nodeRefreshPublish() {
        val publisher: SettingsTreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            SettingsTreeChangeNotifier.TREE_REFRESH_TOPIC
        )
        publisher.notifyTreeRefresh()
    }

}