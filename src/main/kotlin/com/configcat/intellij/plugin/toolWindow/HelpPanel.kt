package com.configcat.intellij.plugin.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel


class HelpPanel : SimpleToolWindowPanel(false, false), Disposable {

    // TODO fix the link to valid links!
    init {
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT
        add(
                    JPanel().apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        alignmentY = Component.TOP_ALIGNMENT
                        layout = GridBagLayout()
                        val gbc = GridBagConstraints()
                        gbc.insets = JBUI.insets(1)
                        gbc.gridx = 0
                        gbc.gridy = 0
                        add(LinkLabel("ConfigCat Docs", "https://configcat.com/docs"), gbc)
                        gbc.gridx = 0
                        gbc.gridy = 1
                        add(LinkLabel("ConfigCat Dashboard", "https://app.configcat.com/"), gbc)
                        gbc.gridx = 0
                        gbc.gridy = 2
                        add(LinkLabel("ConfigCat Intellij Plugin docs", "https://configcat.com/docs"), gbc)
                        gbc.gridx = 0
                        gbc.gridy = 3
                        add(LinkLabel("Report Issue", "https://configcat.com/docs"), gbc)

                    }
        )
    }

    override fun dispose() {
    }
}