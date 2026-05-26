package com.configcat.intellij.plugin.webview

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.messaging.ThemeChangeNotifier
import com.configcat.intellij.plugin.webview.cef.CefLocalRequestHandler
import com.configcat.intellij.plugin.webview.cef.CefStreamResourceHandler
import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

@Serializable
data class AppData(
    val publicApiBaseUrl: String,
    val basicAuthUsername: String,
    val basicAuthPassword: String,
    val dashboardBasePath: String,
    val isAuthorized: Boolean = false,
    val productId: String,
    val productName: String,
    val configId: String,
    val configName: String,
    val environmentId: String,
    val evaluationVersion: String,
    val settingId: String,
)

@Serializable
data class ViewData(
    val view: String,
    val initialTheme: String,
)

enum class ViewType(val type: String) {
    CREATE_CONFIG("createconfig"),
    CREATE_FLAG("createfeatureflag"),
    VIEW_FLAG("featureflagsetting"),
    AUTHORIZATION("authorization")
}

private const val WINDOW_CONFIGCAT_APPDATA = "window.CONFIGCAT_APPDATA ="
private const val WINDOW_CONFIGCAT_APP_VIEW = "window.CONFIGCAT_APP_VIEW ="

class WebViewPanel(
    appData: AppData,
    viewType: ViewType,
    val jsReceiverCallbackFunction: ((returnId: String?) -> Unit)?,
) : JPanel(), Disposable {

    companion object {
        const val DIST_FOLDER_PATH = "dist"
        const val ASSETS_IMAGES_FOLDER_PATH = "assets/images"
        const val SETTING_TYPES_FOLDER_PATH = "setting-types"
        const val INDEX_HTML = "index.html"
        const val MAIN_JS = "main.js"
        const val POLYFILLS_JS = "polyfills.js"
        const val STYLES_CSS = "styles.css"
        const val DECIMAL_SVG = "decimal.svg"
        const val TEXT_SVG = "text.svg"
        const val WHOLE_SVG = "whole.svg"
        const val FEATURE_FLAG_SVG = "feature_flag.svg"
    }

    private val cefClient = JBCefApp.getInstance().createClient()
    private val jBCefBrowser: JBCefBrowser = JBCefBrowserBuilder()
        .setClient(cefClient)
        .setUrl(appData.publicApiBaseUrl)
        .setEnableOpenDevToolsMenuItem(true)
        .setMouseWheelEventEnable(true)
        .build()
    private val jSQuery: JBCefJSQuery = checkNotNull(JBCefJSQuery.create(jBCefBrowser as JBCefBrowserBase))

    init {
        val handleThemeChange = object : ThemeChangeNotifier {
            override fun notifyThemeChange() {
                lookAndFeelChanged()
            }
        }
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(ThemeChangeNotifier.THEME_CHANGE_TOPIC, handleThemeChange)

        layout = CardLayout().apply {
            alignmentX = LEFT_ALIGNMENT
            alignmentY = TOP_ALIGNMENT
            cefClient.setProperty("JS_QUERY_POOL_SIZE", 30)

            val theme = getCurrentTheme()
            val viewData = ViewData(viewType.type, theme)

            initiateCefRequestHandler(viewData, appData)
            setupJavascriptCallback()

            jBCefBrowser.loadURL("${DIST_FOLDER_PATH}/${INDEX_HTML}")

            add(jBCefBrowser.component, BorderLayout.CENTER)
        }
    }

    private fun initiateCefRequestHandler(viewData: ViewData, appData: AppData) {
        val requestHandler = CefLocalRequestHandler()

        requestHandler.addResource(INDEX_HTML) {
            javaClass.classLoader.getResourceAsStream("$DIST_FOLDER_PATH/$INDEX_HTML")?.let {
                val rawHtml = it.readAllBytes()
                    .toString(Charsets.UTF_8)
                    .replace(
                        "$WINDOW_CONFIGCAT_APPDATA {};",
                        "$WINDOW_CONFIGCAT_APPDATA " + Constants.json.encodeToString(appData) + ";"
                    )
                    .replace(
                        "$WINDOW_CONFIGCAT_APP_VIEW {};",
                        "$WINDOW_CONFIGCAT_APP_VIEW " + Constants.json.encodeToString(viewData) + ";"
                    )

                CefStreamResourceHandler(rawHtml.byteInputStream(), "text/html")
            }
        }

        fun addDistResource(resourceName: String, mimeType: String) {
            requestHandler.addResource(resourceName) {
                javaClass.classLoader.getResourceAsStream("$DIST_FOLDER_PATH/$resourceName")
                    ?.let { CefStreamResourceHandler(it, mimeType) }
            }
        }

        addDistResource(MAIN_JS, "text/javascript")
        addDistResource(POLYFILLS_JS, "text/javascript")
        addDistResource(STYLES_CSS, "text/css")

        fun addSettingTypeResource(fileName: String) {
            requestHandler.addResource(fileName) {
                javaClass.classLoader.getResourceAsStream(
                    "$DIST_FOLDER_PATH/$ASSETS_IMAGES_FOLDER_PATH/$SETTING_TYPES_FOLDER_PATH/$fileName"
                )?.let { CefStreamResourceHandler(it, "image/svg+xml") }
            }
        }

        addSettingTypeResource(DECIMAL_SVG)
        addSettingTypeResource(TEXT_SVG)
        addSettingTypeResource(WHOLE_SVG)
        addSettingTypeResource(FEATURE_FLAG_SVG)

        cefClient.addRequestHandler(requestHandler, jBCefBrowser.cefBrowser)

        cefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser?,
                frame: CefFrame?,
                target_url: String?,
                target_frame_name: String?,
            ): Boolean {
                // Return true to cancel the popup and use BrowserUtil.open for external links.
                if (target_url != null) {
                    BrowserUtil.open(target_url)
                } else {
                    ConfigCatNotifier.Notify.error(
                        null,
                        "Failed to open the link in the external browser. The URL is null."
                    )
                }
                return true
            }
        }, jBCefBrowser.cefBrowser)
    }

    private fun receiveHandler(returnId: String?): JBCefJSQuery.Response? {
        jsReceiverCallbackFunction?.invoke(returnId)
        return null
    }

    private fun setupJavascriptCallback() {

        jSQuery.addHandler(::receiveHandler)

        jBCefBrowser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?,
                ) {
                    // enable this if you need the devtools on load.
                    // jBCefBrowser.openDevtools()
                }

                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                    if (browser == null || frame?.isMain != true) return
                    // fire ng load event to properly renderer
                    //override CONFIGCAT_SUCCESS_METHOD to make jsQuery calls
                    //TODO auth success and logout should be here as well
                    browser.executeJavaScript(
                        "document.dispatchEvent(new Event('startNgLoad'));" +
                                "window['configCatSuccessMethod'] = function(returnId) {" +
                                jSQuery.inject("returnId") +
                                "};",
                        browser.url,
                        0,
                    )
                }
            },
            jBCefBrowser.cefBrowser,
        )
    }

    fun lookAndFeelChanged() {
        val theme = getCurrentTheme()
        val message = "{'command': 'themeChange', 'value': '$theme'}"
        jBCefBrowser.cefBrowser.executeJavaScript(
            "window.postMessage($message);",
            jBCefBrowser.cefBrowser.url,
            0
        )
    }

    private fun getCurrentTheme(): String {
        return if (JBColor.isBright()) "light" else "dark"
    }

    override fun dispose() {
        jBCefBrowser.dispose()
        cefClient.dispose()
        jSQuery.dispose()
    }
}

class WebViewLafListener : LafManagerListener {

    override fun lookAndFeelChanged(lafManager: LafManager) {
        val publisher: ThemeChangeNotifier = ApplicationManager.getApplication().messageBus.syncPublisher(
            ThemeChangeNotifier.THEME_CHANGE_TOPIC
        )
        publisher.notifyThemeChange()
    }
}
