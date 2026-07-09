package com.configcat.intellij.plugin

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LoggedErrorProcessor

object TestUtils {

    /**
     * Runs [action] with a no-op [LoggedErrorProcessor] so that expected
     * `thisLogger().error(...)` calls inside production code do not convert
     * into test failures under `LightPlatformTestCase`.
     */
    fun suppressLogErrors(action: () -> Unit) {
        val noOpProcessor = object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<String>,
                t: Throwable?,
            ): Set<LoggedErrorProcessor.Action> = emptySet()
        }
        LoggedErrorProcessor.executeWith<Throwable>(noOpProcessor) { action() }
    }

    /**
     * Safely disposes a [DialogWrapper] without propagating exceptions
     * that may occur during disposal in test environments.
     */
    fun safeDispose(dialog: DialogWrapper) {
        try {
            Disposer.dispose(dialog.disposable)
        } catch (_: Exception) {
        }
    }
}
