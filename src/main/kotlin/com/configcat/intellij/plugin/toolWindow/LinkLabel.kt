package com.configcat.intellij.plugin.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ui.JBColor
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel

class LinkLabel( label: String, private val url: String,) : JLabel() {

    init {
        text = label
        foreground = JBColor.BLUE
        cursor = Cursor(Cursor.HAND_CURSOR)
        icon = AllIcons.Ide.External_link_arrow
        val largerFont = Font(font.name, Font.PLAIN, 14)
        font = largerFont

        addMouseListener( object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                BrowserLauncher.instance.open(url)
            }
        })

    }
}