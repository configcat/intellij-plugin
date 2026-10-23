package com.configcat.intellij.plugin.toolWindow.panel

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import java.awt.Component
import java.awt.Container
import java.awt.GridBagLayout
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel

class HelpPanelTest : LightPlatformTestCase() {

    fun testInit_containsExpectedLinksAndCommentText() {
        val panel = buildPanel()
        val allVisibleText = allText(panel)

        assertTrue(
            "Help panel must include the ConfigCat docs link",
            allVisibleText.any { it.contains("https://configcat.com/docs") }
        )
        assertTrue(
            "Help panel must include the plugin usage guide link",
            allVisibleText.any { it.contains("/integrations/intellij/") }
        )
        assertTrue(
            "Help panel must include the issue tracker link",
            allVisibleText.any { it.contains("https://github.com/configcat/intellij-plugin/issues") }
        )
        assertTrue(
            "Help panel must include the dashboard link",
            allVisibleText.any { it.contains("https://app.configcat.com/") }
        )
        assertTrue(
            "Help panel must include the explanatory comment",
            allVisibleText.any { it.contains("Useful links to open Config Cat's Documentation and Dashboard") }
        )
    }

    fun testInit_addsCenteredContainerPanelWithGridBagLayout() {
        val panel = buildPanel()

        assertEquals("Help panel should add one centered container", 1, panel.componentCount)
        val centeredContainer = panel.getComponent(0)
        assertTrue(
            "Centered container must be a JPanel",
            centeredContainer is JPanel
        )
        assertTrue(
            "Centered container must use GridBagLayout",
            (centeredContainer as JPanel).layout is GridBagLayout
        )
        assertEquals("Centered container should host the UI DSL panel", 1, centeredContainer.componentCount)
    }

    fun testDispose_doesNotThrow() {
        val panel = buildPanel()

        panel.dispose()
    }

    private fun buildPanel(): HelpPanel {
        val panel = HelpPanel()
        Disposer.register(testRootDisposable, panel)
        return panel
    }

    private fun allText(container: Container): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until container.componentCount) {
            val component: Component = container.getComponent(i)
            when (component) {
                is JLabel -> component.text?.takeIf { it.isNotEmpty() }?.let { result += it }
                is JEditorPane -> component.text?.takeIf { it.isNotEmpty() }?.let { result += it }
            }
            if (component is Container) {
                result += allText(component)
            }
        }
        return result
    }
}

