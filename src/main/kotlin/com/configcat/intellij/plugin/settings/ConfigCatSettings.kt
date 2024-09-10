package com.configcat.intellij.plugin.settings

interface ConfigCatSettings {

    var authConfiguration: String
    var dashboardBaseUrl: String
    var publicApiBaseUrl: String

    fun isConfigured(): Boolean
}