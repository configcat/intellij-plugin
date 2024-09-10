package com.configcat.intellij.plugin.services

import com.configcat.publicapi.java.client.ApiClient
import com.configcat.publicapi.java.client.api.*
import com.intellij.openapi.components.Service


@Service(Service.Level.APP)
class ConfigCatService {

    companion object {

        fun createMeService(authConfig: PublicApiConfiguration, basePath: String): MeApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return MeApi(apiClient)
        }

        fun createProductsService(authConfig: PublicApiConfiguration, basePath: String): ProductsApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return ProductsApi(apiClient)
        }

        fun createConfigsService(authConfig: PublicApiConfiguration, basePath: String): ConfigsApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return ConfigsApi(apiClient)
        }

        fun createFeatureFlagsSettingsService(authConfig: PublicApiConfiguration, basePath: String): FeatureFlagsSettingsApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return FeatureFlagsSettingsApi(apiClient)
        }

        fun createEnvironmentsService(authConfig: PublicApiConfiguration, basePath: String): EnvironmentsApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return EnvironmentsApi(apiClient)
        }

    }
}
