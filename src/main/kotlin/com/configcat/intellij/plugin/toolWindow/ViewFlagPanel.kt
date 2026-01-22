package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.webview.cef.CefLocalRequestHandler
import com.configcat.intellij.plugin.webview.cef.CefStreamResourceHandler
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.Label
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

@Serializable
data class ViewData(
    val view: String
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
    val environmentId: String,
    val evaluationVersion: String,
    val settingId: String,
)

class ViewFlagPanel(setting: SettingModel) : SimpleToolWindowPanel(false, false), Disposable {
    companion object {
        const val DIST_FOLDER_PATH = "dist"
        const val INDEX_HTML = "index.html"
        const val MAIN_JS = "main.js"
        const val POLYFILLS_JS = "polyfills.js"
        const val STYLES_CSS = "styles.css"
    }



    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state

    private val cefClient = JBCefApp.getInstance().createClient()
    private val jBCefBrowser: JBCefBrowser = JBCefBrowserBuilder()
        .setClient(cefClient)
        .setUrl("https://test-api.configcat.com/")
        .setEnableOpenDevToolsMenuItem(true)
        .setMouseWheelEventEnable(true)
        .build()
    private val javaScriptEngineProxy: JBCefJSQuery = JBCefJSQuery.create(jBCefBrowser as JBCefBrowserBase)

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        add(
            JPanel(CardLayout()).apply {
                alignmentX = LEFT_ALIGNMENT
                alignmentY = TOP_ALIGNMENT
                if (JBCefApp.isSupported()) {
                    //TODO rewrite this and maybe the tool window as well to executeOnPooledThread

//                    cefClient.setProperty("JS_QUERY_POOL_SIZE", 30)

                    //TODO view and appData fix
                    val viewData = ViewData("featureflagsetting") //TODO view data should be an enum or const
                    val authConf = Constants.decodePublicApiConfiguration(stateConfig.authConfiguration)
                    //TODO not all data is the proper data here.
                    val appData = AppData(
                        stateConfig.publicApiBaseUrl,
                        authConf.basicAuthUserName,
                        authConf.basicAuthPassword,
                        stateConfig.dashboardBaseUrl,
                        "08daf7cd-a3a1-4697-8466-433f4463f8c1",
                        "",
                        setting.configId.toString(),
                        "",
                        "08dcaaf9-23ae-4f6f-804c-2069bdf34cb8",
                        "v2",
                        setting.settingId.toString()
                    )

                    initiateCefRequestHandler(viewData, appData)
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

    private fun initiateCefRequestHandler(  viewData: ViewData, appData: AppData,) {
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
    }

    private fun setupJavascriptCallback() {
        jBCefBrowser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?
                ) {
                    // TODO add is dev IF This should be only in development.
                    jBCefBrowser.openDevtools()

                }
            },
            jBCefBrowser.cefBrowser,
        )
    }

    override fun dispose() {
    }
}