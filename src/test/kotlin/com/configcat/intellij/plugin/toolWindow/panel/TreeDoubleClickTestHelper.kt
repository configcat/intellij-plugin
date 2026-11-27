package com.configcat.intellij.plugin.toolWindow.panel

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

class RecordingAction : AnAction() {
    var executions = 0
        private set

    override fun actionPerformed(e: AnActionEvent) {
        executions++
    }
}

/**
 * Replaces the action registered under [actionId] with a [RecordingAction] and drives double clicks on a [Tree].
 */
class TreeDoubleClickTestHelper(private val actionId: String) {

    val action = RecordingAction()
    private val originalAction: AnAction? = ActionManager.getInstance().getAction(actionId)

    fun register() {
        val actionManager = ActionManager.getInstance()
        if (originalAction != null) actionManager.unregisterAction(actionId)
        actionManager.registerAction(actionId, action)
    }

    fun restore() {
        val actionManager = ActionManager.getInstance()
        actionManager.unregisterAction(actionId)
        if (originalAction != null) actionManager.registerAction(actionId, originalAction)
    }

    fun awaitRows(tree: Tree, expectedRows: Int) {
        tree.setSize(500, 500)
        val deadline = System.currentTimeMillis() + 5000
        while (!isFullyLoaded(tree, expectedRows) && System.currentTimeMillis() < deadline) {
            TreeUtil.promiseExpandAll(tree)
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(50)
        }
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun isFullyLoaded(tree: Tree, expectedRows: Int): Boolean =
        tree.rowCount >= expectedRows && (0 until tree.rowCount).none {
            tree.getPathForRow(it).lastPathComponent.javaClass.simpleName == "LoadingNode"
        }

    fun clickRow(tree: Tree, row: Int, clickCount: Int) {
        val bounds = tree.getRowBounds(row)
        click(tree, bounds.x + 2, bounds.y + bounds.height / 2, clickCount)
    }

    // ClickListener counts consecutive press/release cycles itself, so each click is simulated separately.
    fun click(tree: Tree, x: Int, y: Int, clickCount: Int) {
        val now = System.currentTimeMillis()
        repeat(clickCount) { index ->
            val count = index + 1
            val pressed = MouseEvent(tree, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON1_DOWN_MASK, x, y, count, false, MouseEvent.BUTTON1)
            val released = MouseEvent(tree, MouseEvent.MOUSE_RELEASED, now, 0, x, y, count, false, MouseEvent.BUTTON1)
            tree.mouseListeners.forEach { it.mousePressed(pressed) }
            tree.mouseListeners.forEach { it.mouseReleased(released) }
        }
    }
}
