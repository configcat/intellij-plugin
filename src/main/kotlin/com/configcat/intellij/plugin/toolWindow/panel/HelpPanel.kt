package com.configcat.intellij.plugin.toolWindow.panel

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel


class HelpPanel : SimpleToolWindowPanel(false, false), Disposable {

    init {
        alignmentX = LEFT_ALIGNMENT
        alignmentY = TOP_ALIGNMENT
        add(
            JPanel().apply {
                alignmentX = LEFT_ALIGNMENT
                alignmentY = TOP_ALIGNMENT
                layout = GridBagLayout()
                val gbc = GridBagConstraints()
                gbc.insets = JBUI.insets(1)
                gbc.gridx = 0
                gbc.gridy = 0
                add(LinkLabel("ConfigCat Docs", "https://configcat.com/docs"), gbc)
                gbc.gridx = 0
                gbc.gridy = 1
                add(LinkLabel("How to use the plugin", "https://configcat.com/docs/integrations/intellij/"), gbc)
                gbc.gridx = 0
                gbc.gridy = 2
                add(LinkLabel("Report issues", "https://github.com/configcat/intellij-plugin/issues"), gbc)
                gbc.gridx = 0
                gbc.gridy = 3
                add(LinkLabel("ConfigCat Dashboard", "https://app.configcat.com/"), gbc)
            }
        )
    }

    override fun dispose() {
    }
}

class LinkLabel(label: String, private val url: String) : JLabel() {

    init {
        text = label
        foreground = JBColor.BLUE
        cursor = Cursor(Cursor.HAND_CURSOR)
        icon = AllIcons.Ide.External_link_arrow
        val largerFont = Font(font.name, Font.PLAIN, 14)
        font = largerFont

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                BrowserLauncher.instance.open(url)
            }
        })

    }
}