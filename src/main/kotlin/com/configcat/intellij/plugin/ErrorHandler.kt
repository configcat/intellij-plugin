package com.configcat.intellij.plugin

import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

internal object ErrorHandler {
    private const val DEFAULT_ERROR_MESSAGE: String = "An error occurred. For more information check the logs."
    private const val DEFAULT_AUTH_ERROR_MESSAGE: String = "Authentication failed. Check your authentication settings."

    fun errorNotify(exception: ApiException) {
        errorNotify(exception, null, null)
    }

    fun errorNotify(exception: ApiException,  message: String?,) {
        errorNotify(exception, message, null)
    }

    fun errorNotify(exception: ApiException, project: Project?) {
        errorNotify(exception, null, project)
    }

    fun errorNotify(exception: ApiException, message: String?, project: Project?) {
        val errorMessage: String = if(exception.code == 401) {
            DEFAULT_AUTH_ERROR_MESSAGE
        } else if(message.isNullOrEmpty()) {
            thisLogger().error(DEFAULT_ERROR_MESSAGE, exception)
            DEFAULT_ERROR_MESSAGE
        } else {
            thisLogger().error(DEFAULT_ERROR_MESSAGE, exception)
            message
        }
        ConfigCatNotifier.Notify.error(project, errorMessage)
    }
}