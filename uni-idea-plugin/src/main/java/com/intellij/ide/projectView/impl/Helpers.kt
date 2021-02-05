package com.intellij.ide.projectView.impl

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.intellij.util.ui.tree.TreeUtil

fun extractValueFromNode(node: Any?): Any? {
  val userObject = TreeUtil.getUserObject(node)
  var element: Any? = null
  if (userObject is AbstractTreeNod2<*>) {
    element = userObject.value
  } else if (userObject is NodeDescriptor<*>) {
    element = userObject.element
    if (element is AbstractTreeNod2<*>) {
      element = element.value
    }
  } else if (userObject != null) {
    element = userObject
  }
  return element
}


fun getValueFromNode(node: Any?): Any? {
  return extractValueFromNode(node)
}

