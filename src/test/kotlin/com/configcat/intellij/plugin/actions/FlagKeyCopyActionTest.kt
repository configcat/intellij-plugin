package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.*
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

class FlagKeyCopyActionTest : LightPlatformTestCase() {

    private lateinit var mockSettingsPanel: SettingsPanel
    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState

    override fun setUp() {
        super.setUp()

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        mockState = mockk<ConfigCatApplicationConfig.ConfigCatApplicationConfigState>(relaxed = true)
        every { mockConfig.state } returns mockState
        every { mockState.isConfigured() } returns true

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any(), any()) } just Runs
        every { ConfigCatNotifier.Notify.error(any<String>()) } just Runs

        mockSettingsPanel = mockk(relaxed = true)
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testGetActionUpdateThread_returnsBGT() {
        val action = FlagKeyCopyAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    fun testActionPerformed_nullSelectedElement_notifiesError() {
        val action = FlagKeyCopyAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Copy action") }) }
    }

    fun testActionPerformed_selectedElementNotFlagNode_notifiesError() {
        val action = FlagKeyCopyAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag node")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, null)

        action.actionPerformed(event)

        verify { ConfigCatNotifier.Notify.error(any(), match { it.contains("Copy action") }) }
    }

    fun testActionPerformed_validFlagNode_copiesToClipboard() {
        val action = FlagKeyCopyAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = UUID.randomUUID(),
            key = "my_feature_flag_key",
            name = "My Feature Flag",
            hint = "hint",
            rootName = "TestConfig"
        )
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)

        val mockClipboard = mockk<Clipboard>(relaxed = true)
        val mockToolkit = mockk<Toolkit>(relaxed = true)
        every { mockToolkit.systemClipboard } returns mockClipboard
        mockkStatic(Toolkit::class)
        every { Toolkit.getDefaultToolkit() } returns mockToolkit

        action.actionPerformed(event)

        verify { mockClipboard.setContents(any(), any()) }
    }

    fun testUpdate_nullSelectedElement_disabledAndVisible() {
        val action = FlagKeyCopyAction()
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, null, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when no node is selected", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_nonFlagNode_disabledAndVisible() {
        val action = FlagKeyCopyAction()
        val nonFlagTreeNode = DefaultMutableTreeNode("not a flag")
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, nonFlagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled for non-FlagNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_validFlagNode_enabledAndVisible() {
        val action = FlagKeyCopyAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = UUID.randomUUID(),
            hint = "",
            rootName = "TestConfig"
        )
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertTrue("Presentation must be enabled for FlagNode", presentation.isEnabled)
        assertTrue("Presentation must be visible", presentation.isVisible)
    }

    fun testUpdate_notConfigured_isHidden() {
        every { mockState.isConfigured() } returns false

        val action = FlagKeyCopyAction()
        val flagTreeNode = ActionTestFixtures.createFlagTreeNode(
            settingId = 1,
            configId = UUID.randomUUID(),
            hint = "",
            rootName = "TestConfig"
        )
        val event = ActionTestFixtures.createSettingsEvent(mockSettingsPanel, flagTreeNode, null)
        val presentation = Presentation()
        every { event.presentation } returns presentation

        action.update(event)

        assertFalse("Presentation must be disabled when plugin is not configured", presentation.isEnabled)
        assertFalse("Presentation must be hidden when plugin is not configured", presentation.isVisible)
    }

    fun testCompanionObject_actionIdConstant() {
        assertEquals(
            "CONFIGCAT_COPY_FLAG_KEY_ACTION_ID",
            FlagKeyCopyAction.CONFIGCAT_COPY_FLAG_KEY_ACTION_ID
        )
    }
}
