package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.settings.ConfigCatConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.awt.Component
import java.awt.Container
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JLabel

class ConfigurePluginPanelTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()
        mockkStatic(ShowSettingsUtil::class)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testInit_containsExpectedMessageAndSettingsButton() {
        val panel = ConfigurePluginPanel()

        assertTrue("Panel must use GridBagLayout", panel.layout is GridBagLayout)

        val label = findFirst(panel) { it is JLabel && it.text == "Please configure the ConfigCat plugin." }
        assertNotNull("Configuration hint label must be present", label)

        val settingsButton = findFirst(panel) { it is JButton && it.text == "Settings" }
        assertNotNull("Settings button must be present", settingsButton)
    }

    fun testSettingsButtonClick_opensConfigCatSettingsDialog() {
        val mockShowSettingsUtil = mockk<ShowSettingsUtil>(relaxed = true)
        every { ShowSettingsUtil.getInstance() } returns mockShowSettingsUtil

        val panel = ConfigurePluginPanel()
        val button = findFirst(panel) { it is JButton && it.text == "Settings" } as JButton

        button.doClick()

        verify(exactly = 1) {
            mockShowSettingsUtil.showSettingsDialog(null, ConfigCatConfigurable::class.java)
        }
    }

    private fun findFirst(container: Container, predicate: (Component) -> Boolean): Component? {
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            if (predicate(component)) {
                return component
            }
            if (component is Container) {
                val nested = findFirst(component, predicate)
                if (nested != null) {
                    return nested
                }
            }
        }
        return null
    }
}

