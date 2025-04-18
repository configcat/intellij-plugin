package com.configcat.intellij.plugin.services

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.util.*

@Service(Service.Level.APP)
class ConfigCatNodeDataService {

    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state

    private var productConfigs: MutableMap<UUID, List<ConfigModel>> = mutableMapOf()
    private var configFlags: MutableMap<UUID, List<SettingModel>> = mutableMapOf()


    companion object {

        fun getInstance(): ConfigCatNodeDataService {
            return ApplicationManager.getApplication().getService(ConfigCatNodeDataService()::class.java)
        }
    }

    fun checkAndLoadConfigs(productId: UUID?) : Boolean {
        if(productId == null){
            ConfigCatNotifier.Notify.error("Couldn't load the configs: Missing product ID.")
            thisLogger().error("Couldn't load the configs: Missing product ID.")
            return false
        }
        if(!productConfigs.containsKey(productId)){
            loadConfigs(productId)
        }
        return true
    }

    fun loadConfigs(productId: UUID) {
        val configsService = ConfigCatService.createConfigsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
        val configs = try {
            configsService.getConfigs(productId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }
        productConfigs[productId] = configs
    }

    fun getProductConfigs(productId: UUID) : List<ConfigModel>? {
        return productConfigs[productId]
    }

    fun checkAndLoadFlags(configId: UUID?) : Boolean {
        if(configId == null){
            ConfigCatNotifier.Notify.error("Couldn't load the flags: Missing config ID.")
            thisLogger().error("Couldn't load the flags: Missing config ID.")
            return false
        }
        if(!configFlags.containsKey(configId)){
            loadFlags(configId)
        }
        return true

    }

    fun loadFlags(configId: UUID) {
        val featureFlagsSettingsService = ConfigCatService.createFeatureFlagsSettingsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
        val settings = try {
            featureFlagsSettingsService.getSettings(configId)
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }
        configFlags[configId] = settings
    }

    fun getConfigFlags(configId: UUID) : List<SettingModel>? {
        return configFlags[configId]
    }


    fun resetData(){
        productConfigs = mutableMapOf()
        configFlags = mutableMapOf()
    }
}