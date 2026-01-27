package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.cef.CefLocalRequestHandler
import com.configcat.intellij.plugin.webview.cef.CefStreamResourceHandler
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.application.subscribe
import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.Label
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.executeJavaScript
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandler
import org.cef.handler.CefLifeSpanHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

@Serializable
data class ViewData(
    val view: String,
    val initialTheme: String
)

@Serializable
data class AppData(
    val publicApiBaseUrl: String,
    val basicAuthUsername: String,
    val basicAuthPassword: String,
    val dashboardBasePath: String,
    val productId: String,
    val productName: String,
    val configId: String,
    val configName: String,
    var environmentId: String,
    val evaluationVersion: String,
    val settingId: String,
)
//TODO where is the right place for AppData?

class ViewFlagPanel(appData: AppData) : SimpleToolWindowPanel(false, false), Disposable, LafManagerListener {

    companion object {
        const val DIST_FOLDER_PATH = "dist"
        const val INDEX_HTML = "index.html"
        const val MAIN_JS = "main.js"
        const val POLYFILLS_JS = "polyfills.js"
        const val STYLES_CSS = "styles.css"
    }

    private val cefClient = JBCefApp.getInstance().createClient()
    private val jBCefBrowser: JBCefBrowser = JBCefBrowserBuilder()
        .setClient(cefClient)
        .setUrl(appData.publicApiBaseUrl)
        .setEnableOpenDevToolsMenuItem(true)
        .setMouseWheelEventEnable(true)
        .build()
    private val javaScriptEngineProxy: JBCefJSQuery = JBCefJSQuery.create(jBCefBrowser as JBCefBrowserBase)
    private val lafManger = LafManager.getInstance()
    init {

        LafManagerListener.TOPIC.subscribe(this, this)

        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        add(
            JPanel(CardLayout()).apply {
                alignmentX = LEFT_ALIGNMENT
                alignmentY = TOP_ALIGNMENT
                if (JBCefApp.isSupported()) {
                    cefClient.setProperty("JS_QUERY_POOL_SIZE", 30)

                    val theme = getCurrentTheme()
                    val viewData = ViewData("featureflagsetting", theme ) //TODO view data should be an enum or const

                    initiateCefRequestHandler(viewData, appData)
//                    jBCefBrowser.setOpenLinksInExternalBrowser(true)
                    setupJavascriptCallback()

                    jBCefBrowser.loadURL("${DIST_FOLDER_PATH}/${INDEX_HTML}")

                    add(jBCefBrowser.component, BorderLayout.CENTER)

                    Disposer.register(this@ViewFlagPanel, jBCefBrowser)
                    Disposer.register(this@ViewFlagPanel, cefClient)
                    Disposer.register(this@ViewFlagPanel, javaScriptEngineProxy)

                } else {
                    //TODO better text ... talk to UI team
                    add(Label("No browser suppoerted. Go to dashboard."), BorderLayout.CENTER)
                }
            }
        )
    }

    private fun initiateCefRequestHandler(  viewData: ViewData, appData: AppData) {
        val myRequestHandler = CefLocalRequestHandler(this)
        myRequestHandler.addResource(INDEX_HTML) {
            javaClass.classLoader.getResourceAsStream("${DIST_FOLDER_PATH}/${INDEX_HTML}")?.let {
                //TODO make this nicer
                var rawHtml = it.readAllBytes().toString(Charsets.UTF_8)
                rawHtml = rawHtml.replace("window.CONFIGCAT_APPDATA = {};", "window.CONFIGCAT_APPDATA = " + Constants.json.encodeToString(appData)+ ";")
                rawHtml = rawHtml.replace("window.CONFIGCAT_APP_VIEW = {};", "window.CONFIGCAT_APP_VIEW = " + Constants.json.encodeToString(viewData)+ ";")
                CefStreamResourceHandler(rawHtml.byteInputStream(), "text/html", this)
            }
        }
        myRequestHandler.addResource(MAIN_JS) {
            javaClass.classLoader.getResourceAsStream("${DIST_FOLDER_PATH}/${MAIN_JS}")?.let {
                CefStreamResourceHandler(it, "text/javascript", this)
            }
        }
        myRequestHandler.addResource(POLYFILLS_JS) {
            javaClass.classLoader.getResourceAsStream("${DIST_FOLDER_PATH}/${POLYFILLS_JS}")?.let {
                CefStreamResourceHandler(it, "text/javascript", this)
            }
        }
        myRequestHandler.addResource(STYLES_CSS) {
            javaClass.classLoader.getResourceAsStream("${DIST_FOLDER_PATH}/${STYLES_CSS}")?.let {
                CefStreamResourceHandler(it, "text/css", this)
            }
        }
        cefClient.addRequestHandler(myRequestHandler, jBCefBrowser.cefBrowser)

        cefClient.addLifeSpanHandler(object: CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser?,
                frame: CefFrame?,
                target_url: String?,
                target_frame_name: String?
            ): Boolean {
                println("onBeforePopup $browser $target_url")
                val superResult = super.onBeforePopup(browser, frame, target_url, target_frame_name)
                println("superResult: $superResult")
//                return superResult
                //TODO this works.... but not nice .... think about it!
                // Return true to cancel the popup and use the BrowserUtil.open based on the JBCefBrowserBase.enableExternalBrowserLinks
                BrowserUtil.open(target_url!!)
                return true
            }

            override fun onAfterCreated(browser: CefBrowser?) {
                println("onAfterCreated $browser")
                val superResult = super.onAfterCreated(browser)
                println("superResult: $superResult")
                return superResult
            }

            override fun onAfterParentChanged(browser: CefBrowser?) {
                println("onAfterParentChanged $browser")
                val superResult = super.onAfterParentChanged(browser)
                println("superResult: $superResult")
                return superResult
            }

            override fun onBeforeClose(browser: CefBrowser?) {
                println("onBeforeClose $browser")
                val superResult = super.onBeforeClose(browser)
                println("superResult: $superResult")
                return superResult
            }

        }, jBCefBrowser.cefBrowser)
    }

    private fun setupJavascriptCallback() {
        jBCefBrowser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?
                ) {
                    println("onLoadStart transitionType = $transitionType")
                    // TODO add is dev IF This should be only in development.
                    jBCefBrowser.openDevtools()

                }

                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                    println("onLoadEnd httpStatusCode = $httpStatusCode")
                    // fire ng load event
                    browser?.executeJavaScript(
                                "document.dispatchEvent(new Event('startNgLoad'));",
                        browser.url,
                        0,
                    )
                }
            },
            jBCefBrowser.cefBrowser,
        )
    }

    override fun dispose() {
    }

    override fun lookAndFeelChanged(source: LafManager) {
        println("lookAndFeelChanged $source")
        println(source.lookAndFeelReference.themeId)
        println(lafManger.currentUIThemeLookAndFeel.isDark)
        val theme = getCurrentTheme()
        val message = "{'command': 'themeChange', 'value': '$theme'}"
        jBCefBrowser.cefBrowser.executeJavaScript(
            "window.postMessage($message);",
//            " var event = new Event('message',  { 'command': 'themeChange', 'value': '$theme' }); document.dispatchEvent(event);",
            jBCefBrowser.cefBrowser.url,
            0
        )
    }

    private fun getCurrentTheme(): String {
        var theme = "light"
        if (lafManger.currentUIThemeLookAndFeel.isDark) {
            theme = "dark"
        }
        return theme
    }
}