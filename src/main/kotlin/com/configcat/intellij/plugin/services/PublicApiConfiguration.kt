package com.configcat.intellij.plugin.services

import kotlinx.serialization.Serializable

@Serializable
data class PublicApiConfiguration(
    val basicAuthUserName: String,
    val basicAuthPassword: String
)