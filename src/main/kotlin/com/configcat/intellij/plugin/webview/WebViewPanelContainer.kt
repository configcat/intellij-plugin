package com.configcat.intellij.plugin.webview

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
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
            if (JBCefApp.isSupported()) {
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
                                    "JCEF can be unsupported when the IDE started with an alternative JDK."
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
