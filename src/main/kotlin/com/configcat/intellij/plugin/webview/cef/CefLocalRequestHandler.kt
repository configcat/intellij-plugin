package com.configcat.intellij.plugin.webview.cef

import com.intellij.ide.BrowserUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.net.URI

private typealias CefResourceProvider = () -> CefResourceHandler?

private const val distPrefix = "http://dist"

class CefLocalRequestHandler() : CefRequestHandlerAdapter() {
    private val myResources: MutableMap<String, CefResourceProvider> = HashMap()

    private val rejectingResourceHandler: CefResourceHandler =
        object : CefResourceHandlerAdapter() {
            override fun processRequest(
                request: CefRequest,
                callback: CefCallback,
            ): Boolean {
                callback.cancel()
                return false
            }
        }

    private val resourceRequestHandler =
        object : CefResourceRequestHandlerAdapter() {
            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
            ): CefResourceHandler {
                val url = URI.create(request.url).toURL()
                return try {
                    val fileName = url.path.split("/").last()
                    myResources[fileName]?.let { it() } ?: rejectingResourceHandler
                } catch (e: RuntimeException) {
                    rejectingResourceHandler
                }
            }
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
        if (request?.url.toString().startsWith(distPrefix, true)) {
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
        if (user_gesture) {
            BrowserUtil.open(request?.url.toString())
            return true
        }
        if (request?.transitionType == CefRequest.TransitionType.TT_LINK) {
            return true
        }

        return super.onBeforeBrowse(browser, frame, request, user_gesture, is_redirect)
    }
}
