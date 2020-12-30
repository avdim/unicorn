package com.intellij.ide.projectView.impl

import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.ide.util.treeView.*
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDirectory
import com.intellij.refactoring.move.MoveHandler
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.util.concurrency.InvokerSupplier
import com.intellij.util.ui.tree.TreeUtil
import java.awt.dnd.DnDConstants
import javax.swing.JTree

fun extractValueFromNode(node: Any?): Any? {
  val userObject = TreeUtil.getUserObject(node)
  var element: Any? = null
  if (userObject is AbstractTreeNode<*>) {
    element = userObject.value
  } else if (userObject is NodeDescriptor<*>) {
    element = userObject.element
    if (element is AbstractTreeNode<*>) {
      element = element.value
    }
  } else if (userObject != null) {
    element = userObject
  }
  return element
}

fun canDragElements(elements: Array<Any>, dataContext: DataContext, dragAction: Int): Boolean {
  for (element in elements) {
    if (element is Module) {
      return true
    }
  }
  return dragAction == DnDConstants.ACTION_MOVE && MoveHandler.canMove(dataContext)
}

fun createTreeExpander(treeArg: JTree): TreeExpander {
  return object : DefaultTreeExpander({ treeArg }) {
    val isExpandAllAllowed: Boolean
      get() {
        val model = treeArg.model
        return model == null || model is AsyncTreeModel || model is InvokerSupplier
      }

    override fun isExpandAllVisible(): Boolean {
      return isExpandAllAllowed && Registry.`is`("ide.project.view.expand.all.action.visible")
    }

    override fun canExpand(): Boolean {
      return isExpandAllAllowed && super.canExpand()
    }

    override fun collapseAll(tree: JTree, strict: Boolean, keepSelectionLevel: Int) {
      super.collapseAll(tree, false, keepSelectionLevel)
    }
  }
}

fun getValueFromNode(node: Any?): Any? {
  return extractValueFromNode(node)
}

