package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.services.FlagViewOpenHandler
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode


class FlagViewOpenAction : ConfigCatBaseAnAction() {
    companion object {
        const val CONFIGCAT_OPEN_FF_ACTION_ID = "CONFIGCAT_OPEN_FF_ACTION_ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val selectedNode = selectedElement?.userObject

        if ((selectedNode == null || selectedNode !is FlagNode)) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open Feature Flag action could not be executed without a selected Flag Node."
            )
            return
        }

        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()
        if (configModel == null) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open Feature Flag action could not be executed without a connected Config."
            )
            return
        }

        FlagViewOpenHandler.openFlagView(
            e.project,
            state,
            configModel,
            selectedNode.setting.settingId,
            selectedNode.setting.name
        )
    }

    override fun update(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<SettingsPanel>()?.getSelectedNode()
        val configModel = e.project?.service<SettingsPanel>()?.getConnectedConfig()

        val isEnabled = selectedElement?.userObject is FlagNode && configModel != null
        updateVisibility(e, isEnabled)
    }
}

