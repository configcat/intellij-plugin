package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.toolWindow.ConfigCatToolWindowFactory
import com.configcat.intellij.plugin.webview.AppData
import com.configcat.publicapi.java.client.model.EnvironmentModel
import com.intellij.collaboration.ui.util.name
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class EnvironmentSelectDialog(val project: Project?, private val environments: List<EnvironmentModel>, val appData: AppData, val settingName: String ): DialogWrapper(true) {

    private val environmentsDropDown = ComboBox<EnvironmentDropDown>()


    init {
        title = "Select an Environment"
        okAction.name="Select"
        init()
    }

    override fun createCenterPanel(): JComponent {

        //TODO select env from dropdown
        // return with env ID
        val environmentsDropDownComparator =
            Comparator<EnvironmentDropDown> { o1, o2 -> o1.id.compareTo(o2.id) }
        val sortedComboBoxModel = SortedComboBoxModel(environmentsDropDownComparator)

        val environmentDropDownList: List<EnvironmentDropDown> =  environments.map { it -> EnvironmentDropDown(it.name, it.environmentId.toString()) }


        sortedComboBoxModel.addAll(environmentDropDownList)
        environmentsDropDown.model = sortedComboBoxModel
        environmentsDropDown.selectedItem = environmentDropDownList[0]

        val dialogPanel : DialogPanel = panel {
            row{
                text("Select in which Environment should we open the Feature Flag.")
            }
            row("Environment:") {
                cell(environmentsDropDown)
                    .validationOnInput {
                        return@validationOnInput environmentsDropDownValidation()
                    }
                    .validationOnApply {
                        return@validationOnApply environmentsDropDownValidation()
                    }
                    .columns(COLUMNS_MEDIUM)

            }
        }

        return dialogPanel
    }

    fun environmentsDropDownValidation(): ValidationInfo? {
        if(environmentsDropDown.model.selectedItem ==  null){
            return ValidationInfo("Invalid environment.", environmentsDropDown)
        }
        return null
    }

    override fun doOKAction() {
        // set selected env id to appData and create FFView
        val selectedEnvironment = environmentsDropDown.selectedItem as EnvironmentDropDown
        appData.environmentId = selectedEnvironment.id
        project?.let {
            val toolWindow =
                ToolWindowManager.getInstance(it).getToolWindow(ConfigCatToolWindowFactory.CONFIGCAT_TOOL_WINDOW_ID)
            val myToolWindow = ConfigCatToolWindowFactory.ConfigCatFeatureFlagsViewToolWindow(project, toolWindow!!,appData )
            val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(),
                "$settingName ($selectedEnvironment)", false)
            content.isCloseable = true
            toolWindow.contentManager.addContent(content)
            toolWindow.contentManager.setSelectedContent(content)
        }
        super.doOKAction()
    }

    data class EnvironmentDropDown(val name: String, val id: String) : Comparable<EnvironmentDropDown>{

        override fun compareTo(other: EnvironmentDropDown): Int {
            return this.id.compareTo(other.id)
        }

        override fun toString(): String {
            return name
        }
    }

}