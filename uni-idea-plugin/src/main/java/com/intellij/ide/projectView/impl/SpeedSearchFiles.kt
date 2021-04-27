package com.intellij.ide.projectView.impl

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.util.text.StringUtil
import java.util.*
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class SpeedSearchFiles(tree: JTree?) : TreeSpeedSearch2(tree) {
  override fun isMatchingElement(element: Any, pattern: String): Boolean {
    val userObject = ((element as TreePath).lastPathComponent as DefaultMutableTreeNode).userObject
    return if (userObject is PsiDirectoryNode) {
      var str: String? = getElementText(element) ?: return false
      str = StringUtil.toLowerCase(str)
      if (pattern.indexOf('.') >= 0) {
        return compare(str, pattern)
      }
      val tokenizer = StringTokenizer(str, ".")
      while (tokenizer.hasMoreTokens()) {
        val token = tokenizer.nextToken()
        if (compare(token, pattern)) {
          return true
        }
      }
      false
    } else {
      super.isMatchingElement(element, pattern)
    }
  }
}