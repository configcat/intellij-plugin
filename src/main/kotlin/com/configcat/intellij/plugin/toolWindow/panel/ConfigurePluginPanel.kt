package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.settings.ConfigCatConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ConfigurePluginPanel : JPanel() {

    init {
        layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = JBUI.insets(1)
        gbc.gridx = 0
        gbc.gridy = 0
        add(JLabel("Please configure the ConfigCat plugin."), gbc)
        gbc.gridy = 1
        add(JButton("Settings").apply {
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, ConfigCatConfigurable::class.java)
            }
        }, gbc)
    }

}