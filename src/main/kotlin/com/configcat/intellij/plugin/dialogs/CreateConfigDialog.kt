package com.configcat.intellij.plugin.dialogs

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.CreateConfigRequest
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.configcat.publicapi.java.client.model.ProductModel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JTextField

class CreateConfigDialog(val project: Project?, val product: ProductModel): DialogWrapper(true) {

    private val nameTextField = JTextField()
    private val descriptionTextField = JTextField()

    init {
        title = "Create Config"
        init()
    }

    override fun createCenterPanel(): JComponent {

        val dialogPanel : DialogPanel = panel {
            row{
                text("Create a new Config in the ${product.name} Product.")
            }
            row("Name"){
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
            row("Description"){
                cell(descriptionTextField)
                    .columns(COLUMNS_MEDIUM)
            }
        }

        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        if(nameTextField.text.isNullOrEmpty()){
            return ValidationInfo("Invalid name", nameTextField)
        }
        return null
    }

    override fun doOKAction() {
        val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state

        val productId = product.productId
        if(productId == null) {
            ConfigCatNotifier.Notify.error(project,"Config create failed. Missing product ID.")
            return
        }

        val configsService = ConfigCatService.createConfigsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
        val createConfigRequest =  CreateConfigRequest()
        createConfigRequest.name = nameTextField.text
        createConfigRequest.description = descriptionTextField.text
        createConfigRequest.evaluationVersion = EvaluationVersion.V2

        try {
            configsService.createConfig(productId, createConfigRequest)
            val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
            configCatNodeDataService.loadConfigs(productId)
        }catch (e:ApiException){
            ErrorHandler.errorNotify(e, "Config create failed. For more information check the logs.", project)
        }
        super.doOKAction()
    }

}