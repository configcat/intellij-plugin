package com.configcat.intellij.plugin.services

import com.configcat.publicapi.java.client.ApiClient
import com.configcat.publicapi.java.client.api.*
import com.intellij.openapi.components.Service


@Service(Service.Level.APP)
class ConfigCatService {

    companion object {

        private fun createApiClient(authConfig: PublicApiConfiguration, basePath: String): ApiClient {
            val apiClient = ApiClient()
            apiClient.setBasePath(basePath)
            apiClient.setUsername(authConfig.basicAuthUserName)
            apiClient.setPassword(authConfig.basicAuthPassword)
            return apiClient
        }

        fun createMeService(authConfig: PublicApiConfiguration, basePath: String): MeApi {
            return MeApi(createApiClient(authConfig, basePath))
        }

        fun createProductsService(authConfig: PublicApiConfiguration, basePath: String): ProductsApi {
            return ProductsApi(createApiClient(authConfig, basePath))
        }

        fun createConfigsService(authConfig: PublicApiConfiguration, basePath: String): ConfigsApi {
            return ConfigsApi(createApiClient(authConfig, basePath))
        }

        fun createFeatureFlagsSettingsService(
            authConfig: PublicApiConfiguration,
            basePath: String,
        ): FeatureFlagsSettingsApi {
            return FeatureFlagsSettingsApi(createApiClient(authConfig, basePath))
        }

        fun createEnvironmentsService(authConfig: PublicApiConfiguration, basePath: String): EnvironmentsApi {
            return EnvironmentsApi(createApiClient(authConfig, basePath))
        }

    }
}
