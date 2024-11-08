package com.configcat.intellij.plugin.toolWindow

import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure

class FlagTreeStructure(private val rootElement: SimpleNode) : SimpleTreeStructure() {

    override fun getRootElement(): Any {
        return rootElement
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return true
    }
}