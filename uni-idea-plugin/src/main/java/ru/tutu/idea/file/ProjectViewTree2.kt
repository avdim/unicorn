// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.projectView.impl.ProjectViewRenderer
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.DirtyUI
import com.intellij.ui.JBColor
import com.intellij.ui.popup.HintUpdateSupply
import com.unicorn.Uni
import java.awt.Color
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel

open class ProjectViewTree2(model: TreeModel?) : DnDAwareTree2(null as TreeModel?/*TODO null*/) {

  init {
    isLargeModel = true
    setModel(model)
    HintUpdateSupply.installDataContextHintUpdateSupply(this)
  }

  override fun setToggleClickCount(count: Int) {
    if (count != 2) {
      Uni.log.error {
        "setToggleClickCount: unexpected count = $count"
      }
    }
    super.setToggleClickCount(count)
  }

  override fun isFileColorsEnabled(): Boolean {
    return true
  }

  @DirtyUI
  override fun getFileColorFor(obj: Any): Color? {
    var obj: Any? = obj
    if (obj is DefaultMutableTreeNode) {
      obj = obj.userObject
    }
    if (obj is AbstractTreeNode<*>) {
      val value = obj.value
      if (value is PsiElement) {
        val psi = value
        if (!psi.isValid) {
          return JBColor.RED
        }
        val file = PsiUtilCore.getVirtualFile(psi)
        if (file != null) {
          if (file.isDirectory) {
            return JBColor.WHITE
          } else {
            return JBColor.WHITE
          }
        }
      }
    }
    return JBColor.BLACK
  }

}
