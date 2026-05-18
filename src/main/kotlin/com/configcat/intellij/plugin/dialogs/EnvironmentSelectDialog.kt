package com.configcat.intellij.plugin.dialogs

import com.configcat.publicapi.java.client.model.EnvironmentModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.Action
import javax.swing.JComponent

class EnvironmentSelectDialog(
    val project: Project?,
    private val environments: List<EnvironmentModel>,
) : DialogWrapper(true) {

    private val environmentsDropDown = ComboBox<EnvironmentDropDown>()
    var selectedEnvironment: EnvironmentDropDown? = null

    init {
        title = "Select an Environment"
        okAction.putValue(Action.NAME, "Select")
        init()
    }

    override fun createCenterPanel(): JComponent {

        val environmentsDropDownComparator =
            Comparator<EnvironmentDropDown> { o1, o2 -> o1.id.compareTo(o2.id) }
        val sortedComboBoxModel = SortedComboBoxModel(environmentsDropDownComparator)

        val environmentDropDownList: List<EnvironmentDropDown> =
            environments.map { EnvironmentDropDown(it.name, it.environmentId.toString()) }


        sortedComboBoxModel.addAll(environmentDropDownList)
        environmentsDropDown.model = sortedComboBoxModel
        if (!environmentDropDownList.isEmpty()) {
            environmentsDropDown.selectedItem = environmentDropDownList[0]
        }

        val dialogPanel: DialogPanel = panel {
            row {
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
        if (environmentsDropDown.model.selectedItem == null) {
            return ValidationInfo("Invalid environment.", environmentsDropDown)
        }
        return null
    }

    override fun doOKAction() {
        selectedEnvironment = environmentsDropDown.selectedItem as EnvironmentDropDown
        super.doOKAction()
    }

    data class EnvironmentDropDown(val name: String, val id: String) : Comparable<EnvironmentDropDown> {

        override fun compareTo(other: EnvironmentDropDown): Int {
            return this.id.compareTo(other.id)
        }

        override fun toString(): String {
            return name
        }
    }

}

