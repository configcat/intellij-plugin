package com.configcat.intellij.plugin.webview

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.jcef.*
import com.intellij.util.ui.JBFont
import java.awt.CardLayout
import javax.swing.JPanel

class WebViewPanelContainer(
    appData: AppData,
    viewType: ViewType,
    val jsReceiverCallbackFunction: ((returnId: String?) -> Unit)?,
    parentDisposable: Disposable
) : JPanel(), Disposable {

    override fun dispose() {
    // no-op, the parentDisposable will handle disposal of this panel and its children
    }


    init {

        layout = CardLayout().apply {
            alignmentX = LEFT_ALIGNMENT
            alignmentY = TOP_ALIGNMENT
            if (isJcefAvailable(this@WebViewPanelContainer.javaClass.classLoader) && JBCefApp.isSupported()) {
                add(WebViewPanel(appData, viewType, jsReceiverCallbackFunction, this@WebViewPanelContainer));
            } else {
                add(
                    panel {
                        row {
                            icon(AllIcons.General.Error)
                            label("JCEF (Java Chromium Embedded Framework) is not supported.").applyToComponent {
                                this.font = JBFont.h2()
                            }
                        }
                        row {
                            text(
                                "This plugin requires JCEF to load web views. " +
                                    "Enable the Web Browser (JCEF) plugin if available and use the IDE's bundled runtime."
                            )
                        }
                        row {
                            text(
                                "You can still manage your Feature Flags on the " +
                                    "<a href=\"https://app.configcat.com/\">ConfigCat Dashboard</a>."
                            )
                        }
                    }
                )
            }
        }
        Disposer.register(parentDisposable, this);
    }
}

internal fun isJcefAvailable(classLoader: ClassLoader): Boolean =
    classLoader.getResource("com/intellij/ui/jcef/JBCefApp.class") != null
