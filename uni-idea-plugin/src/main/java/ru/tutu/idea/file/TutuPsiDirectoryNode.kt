// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.*
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import java.util.*

class TutuPsiDirectoryNode @JvmOverloads constructor(
  project: Project?,
  value: PsiDirectory,
  viewSettings: ViewSettings?,
  filter: PsiFileSystemItemFilter? = null
) : PsiDirectoryNode2(project, value, viewSettings, filter) {

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    val baseDir = virtualFile!!
    val psiManager = PsiManager.getInstance(project!!)
    val nodes: MutableList<BasePsiNode<*>> = ArrayList()
    val files = baseDir.children
    for (file in files) {
      val psiFile = psiManager.findFile(file)
      if (psiFile != null) {
        nodes.add(PsiFileNode(project, psiFile, settings))
      }

      val psiDir = psiManager.findDirectory(file)
      if (psiDir != null) {
        nodes.add(TutuPsiDirectoryNode(project, psiDir, settings))
      }
    }
    return nodes
  }

}
