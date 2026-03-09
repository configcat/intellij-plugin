package com.configcat.intellij.plugin.toolWindow.panel

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.panel
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel


class HelpPanel : SimpleToolWindowPanel(false, false), Disposable {

    init {
        val panel = panel {
            row {
                text("Useful links to open Config Cat's Documentation and Dashboard, and allows you to report issues.")
            }
            indent {
                row {
                    text("<a href=\"https://configcat.com/docs\">ConfigCat Docs</a>")
                }
                row {
                    text("<a href=\"https://configcat.com/docs/integrations/intellij/\">How to use the plugin</a>")
                }
                row {
                    text("<a href=\"https://github.com/configcat/intellij-plugin/issues\">Report issues</a>")
                }
                row {
                    text("<a href=\"https://app.configcat.com/\">ConfigCat Dashboard</a>")
                }
            }
        }
        panel.alignmentX = CENTER_ALIGNMENT
        panel.alignmentY = TOP_ALIGNMENT
        add(panel)
    }

    override fun dispose() {
    }
}