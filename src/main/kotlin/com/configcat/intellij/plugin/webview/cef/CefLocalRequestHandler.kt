package com.configcat.intellij.plugin.webview.cef

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.net.URI

private typealias CefResourceProvider = () -> CefResourceHandler?

private const val DIST_PREFIX = "http://dist"

class CefLocalRequestHandler : CefRequestHandlerAdapter() {
    private val myResources: MutableMap<String, CefResourceProvider> = HashMap()

    private val rejectingResourceHandler: CefResourceHandler =
        object : CefResourceHandlerAdapter() {
            override fun processRequest(
                request: CefRequest,
                callback: CefCallback,
            ): Boolean {
                return rejectRequest { callback.cancel() }
            }
        }

    private val resourceRequestHandler =
        object : CefResourceRequestHandlerAdapter() {
            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
            ): CefResourceHandler {
                return resolveResourceHandler(request.url)
            }
        }

    internal fun isDistUrl(url: String?): Boolean {
        return url?.startsWith(DIST_PREFIX, true) == true
    }

    internal fun shouldConsumeNavigation(
        userGesture: Boolean,
        transitionType: CefRequest.TransitionType?,
    ): Boolean {
        return userGesture || transitionType == CefRequest.TransitionType.TT_LINK
    }

    internal fun resolveResourceHandler(url: String?): CefResourceHandler {
        return try {
            val fileName = URI.create(url.orEmpty()).toURL().path.split("/").last()
            myResources[fileName]?.let { it() } ?: rejectingResourceHandler
        } catch (e: RuntimeException) {
            thisLogger().warn("Failed to resolve resource handler for url: $url", e)
            rejectingResourceHandler
        }
    }

    internal fun isRejectingHandler(resourceHandler: CefResourceHandler): Boolean {
        return resourceHandler === rejectingResourceHandler
    }

    internal fun rejectRequest(onCancel: () -> Unit): Boolean {
        onCancel()
        return false
    }

    fun addResource(
        resourcePath: String,
        resourceProvider: CefResourceProvider,
    ) {
        myResources[resourcePath] = resourceProvider
    }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?,
    ): CefResourceRequestHandler? {
        if (isDistUrl(request?.url)) {
            return resourceRequestHandler
        }
        return null

    }

    override fun onBeforeBrowse(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        user_gesture: Boolean,
        is_redirect: Boolean,
    ): Boolean {
        if (request == null) {
            return super.onBeforeBrowse(browser, frame, null, user_gesture, is_redirect)
        }

        if (shouldConsumeNavigation(user_gesture, request.transitionType)) {
            if (user_gesture) {
                BrowserUtil.open(request.url.toString())
            }
            return true
        }

        return super.onBeforeBrowse(browser, frame, request, user_gesture, is_redirect)
    }
}
