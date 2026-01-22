package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.ConfigCatPanel
import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.toolWindow.ConfigNode
import com.configcat.intellij.plugin.toolWindow.FlagNode
import com.configcat.intellij.plugin.toolWindow.ProductNode
import com.configcat.intellij.plugin.toolWindow.ViewFlagPanel
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.EvaluationVersion

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.ModalTaskOwner.project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import info.debatty.java.stringsimilarity.examples.nischay21
import java.awt.BorderLayout
import javax.swing.tree.DefaultMutableTreeNode


class OpenFeatureFlagAction : AnAction() {
    companion object {
        const val CONFIGCAT_OPEN_FF_ACTION_ID = "CONFIGCAT_OPEN_FF_ACTION_ID"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        if (selectedNode == null || selectedNode !is FlagNode) {
            ConfigCatNotifier.Notify.error(
                e.project,
                "Open Feature Flag action could not be executed without a selected Flag Node."
            )
            return
        }
        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate =
            ConfigCatApplicationConfig.getInstance().state

        val configParent = selectedNode.parent as ConfigNode
        val productParent = configParent.parent as ProductNode

        val environmentsService = ConfigCatService.createEnvironmentsService(
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration),
            stateConfig.publicApiBaseUrl
        )
        val environments = try {
            environmentsService.getEnvironments(productParent.product.productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }
        //TODO enviroment select ....should be a thing
        val evaluationVersion = configParent.config.evaluationVersion ?: EvaluationVersion.V1
        val orgId = productParent.product.organization?.organizationId

        //TODO open what should be opened
        e.project?.let {
            val toolWindow =
                ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            //TODO try to add new panel .. hopefully closable with web view init
//           //TODO this should be removed the View Action should create and add the content
            val myToolWindow = ConfigCatToolWindowFactory.ConfigCatFeatureFlagsViewToolWindow(e.project!!, toolWindow!!, selectedNode.setting)
            val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), selectedNode.setting.name, false)
            content.isCloseable = true
            toolWindow.contentManager.addContent(content)
        }

    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val selectedElement: DefaultMutableTreeNode? = e.project?.service<ConfigCatPanel>()?.getSelectedNode()

        val selectedNode = selectedElement?.userObject
        val isEnabled = selectedNode != null &&  selectedNode is FlagNode
        e.presentation.isEnabled = isEnabled
        e.presentation.isVisible = true
    }


}