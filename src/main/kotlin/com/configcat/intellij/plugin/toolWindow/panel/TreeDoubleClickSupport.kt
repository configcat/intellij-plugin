package com.configcat.intellij.plugin.toolWindow.panel

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.treeStructure.Tree
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode

object TreeDoubleClickSupport {

    /**
     * Executes the action with [actionId] when a leaf node accepted by [isTarget] is double clicked.
     */
    fun install(tree: Tree, actionId: String, isTarget: (Any?) -> Boolean) {
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val path = tree.getPathForLocation(event.x, event.y) ?: return false
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return false
                if (!tree.model.isLeaf(node) || !isTarget(node.userObject)) return false

                tree.selectionPath = path
                val action = ActionManager.getInstance().getAction(actionId) ?: return false
                ActionUtil.invokeAction(action, tree, ActionPlaces.TOOLWINDOW_CONTENT, event, null)
                return true
            }
        }.installOn(tree)
    }
}
