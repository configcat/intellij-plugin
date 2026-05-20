package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.dialogs.CreateFlagDialog
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.services.FlagViewOpenHandler
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.configcat.publicapi.java.client.model.ConfigModel


class FlagCreateAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_FLAG_CREATE_ACTION_ID = "CONFIGCAT_FLAG_CREATE_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        if (configModel == null) {
            ConfigCatNotifier.Notify.error(e.project, "Create action could not be executed without a connected Config.")
            return
        }

        val dialog = CreateFlagDialog(e.project, configModel)
        val isSuccessful = dialog.showAndGet()
        val flagIdToSelect = if (isSuccessful) {
            openCreatedFlagView(configModel, dialog.createdFlagId, e)
            dialog.createdFlagId
        } else {
            thisLogger().error("Flag creation was not successful. Skipping feature flag view opening.")
            null
        }
        nodeRefreshPublish(flagIdToSelect)
    }

    override fun update(e: AnActionEvent) {
        updateVisibility(e, true)
    }

    private fun nodeRefreshPublish(flagIdToSelect: Int? = null) {
        val publisher: SettingsTreeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            SettingsTreeChangeNotifier.TREE_REFRESH_TOPIC
        )
        publisher.notifyTreeRefresh(flagIdToSelect)
    }

    private fun openCreatedFlagView(configModel: ConfigModel, createdFlagId: Int?, e: AnActionEvent) {
        if (createdFlagId == null) {
            return
        }

        val featureFlagsSettingsService = ConfigCatService.createFeatureFlagsSettingsService(
            Constants.decodePublicApiConfiguration(state.authConfiguration),
            state.publicApiBaseUrl
        )
        val settingName = try {
            featureFlagsSettingsService.getSetting(createdFlagId).name
        } catch (exception: ApiException) {
            thisLogger().error("Failed to resolve setting for created flag ID: $createdFlagId. Skipping view opening.", exception)
            return
        }
        if (settingName.isEmpty()) {
            thisLogger().error("Created flag setting name is missing for ID: $createdFlagId. Skipping view opening.")
            return
        }

        FlagViewOpenHandler.openFlagView(
            e.project,
            state,
            configModel,
            createdFlagId,
            settingName
        )
    }
}

