package com.intellij.ide.projectView.impl

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.refactoring.move.MoveHandler
import com.intellij.util.ui.tree.TreeUtil
import java.awt.dnd.DnDConstants

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

fun canDragElements(elements: Array<Any?>, dataContext: DataContext, dragAction: Int): Boolean {
  for (element in elements) {
    if (element is Module) {
      return true
    }
  }
  return dragAction == DnDConstants.ACTION_MOVE && MoveHandler.canMove(dataContext)
}
