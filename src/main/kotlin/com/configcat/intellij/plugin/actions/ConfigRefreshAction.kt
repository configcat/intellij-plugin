package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager

class ConfigRefreshAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_CONFIG_REFRESH_ACTION_ID = "CONFIGCAT_CONFIG_REFRESH_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Reset the stored data and call reload on panel
        val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
        configCatNodeDataService.resetProductConfigsData()
        refreshPublish()
    }

    private fun refreshPublish() {
        val publisher: ProductsConfigsTreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC
        )
        publisher.notifyTreeRefresh()
    }

    override fun update(e: AnActionEvent) {
        updateVisibility(e, true)
    }

}