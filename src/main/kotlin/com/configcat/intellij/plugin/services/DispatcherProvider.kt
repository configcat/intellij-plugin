package com.configcat.intellij.plugin.services

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Service(Service.Level.APP)
class DispatcherProvider {

    companion object {
        fun getInstance(): DispatcherProvider =
            com.intellij.openapi.application.ApplicationManager.getApplication().getService(DispatcherProvider::class.java)
    }

    open fun default(): CoroutineDispatcher = Dispatchers.Default

    open fun edt(): CoroutineContext = Dispatchers.EDT
}
