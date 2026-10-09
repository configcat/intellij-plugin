package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class ConfigCatBaseAnAction : AnAction() {

    protected val state: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
        get() = ConfigCatApplicationConfig.getInstance().state

    protected fun updateVisibility(e: AnActionEvent, isEnabledWhenConfigured: Boolean) {
        val isConfigured = state.isConfigured()
        e.presentation.isVisible = isConfigured
        e.presentation.isEnabled = isConfigured && isEnabledWhenConfigured
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}


