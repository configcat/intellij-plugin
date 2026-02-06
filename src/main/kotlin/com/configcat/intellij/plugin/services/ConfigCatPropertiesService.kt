package com.configcat.intellij.plugin.services

import com.configcat.publicapi.java.client.model.ConfigModel
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service


@Service(Service.Level.APP)
class ConfigCatPropertiesService {

    companion object {
        const val CONFIGCAT_CONNECTED_CONFIG_ID = "CONFIGCAT_CONNECTED_CONFIG_ID"

        fun getInstance(): ConfigCatPropertiesService {
            return ApplicationManager.getApplication().getService(ConfigCatPropertiesService()::class.java)
        }
    }

    val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance()

    fun getConnectedConfig(): String? {
        return propertiesComponent.getValue(CONFIGCAT_CONNECTED_CONFIG_ID)
    }

    fun setConnectedConfig(configId: String) {
        println("set config id: $configId")
        propertiesComponent.setValue(CONFIGCAT_CONNECTED_CONFIG_ID, configId)
    }

}