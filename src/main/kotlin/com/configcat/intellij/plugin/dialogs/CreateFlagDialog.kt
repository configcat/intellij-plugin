package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.CreateSettingInitialValues
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JTextField

class CreateFlagDialog(val config: ConfigModel): DialogWrapper(true) {

  private val nameTextField = JTextField()
    private val keyTextField = JTextField()
    private val hintTextField = JTextField()
    private val flagTypeDropDown = ComboBox<SettingTypeDropDown>()

    init {
        title = "Create Flag"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val settingTypes: List<SettingTypeDropDown> =  listOf(
            SettingTypeDropDown("Feature Flag (boolean)", CreateSettingInitialValues.SettingTypeEnum.BOOLEAN),
            SettingTypeDropDown("Text (string)", CreateSettingInitialValues.SettingTypeEnum.STRING),
            SettingTypeDropDown("Whole number (integer)", CreateSettingInitialValues.SettingTypeEnum.INT),
            SettingTypeDropDown("Decimal number (double)", CreateSettingInitialValues.SettingTypeEnum.DOUBLE),
        )

        val settingTypeDropDownComparator =
            Comparator<SettingTypeDropDown> { o1, o2 -> o1.compareTo(o2) }
        val sortedComboBoxModel = SortedComboBoxModel(settingTypeDropDownComparator)
        sortedComboBoxModel.addAll(settingTypes)
        flagTypeDropDown.model = sortedComboBoxModel

        val dialogPanel  = panel {
            row{
                text("Create a new Setting in the ${config.name} Config.")
            }
            row("Setting type"){
                cell(flagTypeDropDown)
                    .validationOnInput {
                        if(flagTypeDropDown.model.selectedItem ==  null){
                            return@validationOnInput ValidationInfo("Invalid Flag type", flagTypeDropDown)
                        }
                        myOKAction.isEnabled = true
                        null
                    }
                    .columns(COLUMNS_MEDIUM)

            }
            row("Name for hoomans"){
                cell(nameTextField)
                    .validationOnInput {
                        if(nameTextField.text.isNullOrEmpty() ){
                            return@validationOnInput ValidationInfo("Invalid name", nameTextField)
                        }
                        myOKAction.isEnabled = true
                        null
                    }
                    .columns(COLUMNS_MEDIUM)
            }
            row("Key for programs"){
                cell(keyTextField)
                    .validationOnInput {
                        if(keyTextField.text.isNullOrEmpty() ){
                            return@validationOnInput ValidationInfo("Invalid key", keyTextField)
                        }
                        myOKAction.isEnabled = true
                        null
                    }
                    .columns(COLUMNS_MEDIUM)
            }
            row("Hint"){
                cell(hintTextField)
                    .columns(COLUMNS_MEDIUM)
            }

        }

        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        if(nameTextField.text.isNullOrEmpty()){
            return ValidationInfo("Invalid name", nameTextField)
        }
        if(keyTextField.text.isNullOrEmpty()){
            return ValidationInfo("Invalid key", keyTextField)
        }
        if(flagTypeDropDown.model.selectedItem ==  null){
            return ValidationInfo("Invalid Flag type", flagTypeDropDown)
        }
        return null
    }


    override fun doOKAction() {
        super.doOKAction()

        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state

        val configId = config.configId
        if(configId == null) {
            ConfigCatNotifier.Notify.error("Flag create failed. Missing config ID.")
            return
        }

        val settingService = ConfigCatService.createFeatureFlagsSettingsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
        val createSettingInitialValues =  CreateSettingInitialValues()
        createSettingInitialValues.name = nameTextField.text
        createSettingInitialValues.key = keyTextField.text
        createSettingInitialValues.hint = hintTextField.text
        val settingTypeDropDownValue = flagTypeDropDown.model.selectedItem as SettingTypeDropDown
        createSettingInitialValues.settingType = settingTypeDropDownValue.type

        try {
            settingService.createSetting(configId, createSettingInitialValues)
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadFlags(configId)
        }catch (e:Exception){
            ConfigCatNotifier.Notify.error("Flag create failed. For more information check the logs.")
            thisLogger().error("Flag create failed.", e)
        }

    }

    data class SettingTypeDropDown(val name: String, val type: CreateSettingInitialValues.SettingTypeEnum) : Comparable<SettingTypeDropDown>{

        override fun compareTo(other: SettingTypeDropDown): Int {
            return this.type.compareTo(other.type)
        }

        override fun toString(): String {
            return name
        }
    }
}