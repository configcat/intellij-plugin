package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.messaging.TreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager

class RefreshAction: RefreshAction() {
    companion object {
        const val CONFIGCAT_REFRESH_ACTION_ID = "CONFIGCAT_REFRESH_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Reset the stored data and call reload on panel
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
        configCatNodeDataService.resetData()
        refreshPublish()
    }

    private fun refreshPublish() {
        val publisher: TreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            TreeChangeNotifier.TREE_CHANGE_TOPIC)
        publisher.notifyTreeRefresh()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = true
    }


}