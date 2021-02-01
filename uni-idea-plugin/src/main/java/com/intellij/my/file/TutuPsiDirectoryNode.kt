// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.unicorn.Uni
import java.util.*

class TutuPsiDirectoryNode @JvmOverloads constructor(
  val project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings?,
  filter: PsiFileSystemItemFilter? = null
) : PsiDirectoryNode2(value, viewSettings, filter) {

  override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> {
    val baseDir = virtualFile!!
    val psiManager = PsiManager.getInstance(Uni.todoDefaultProject)
    val nodes: MutableList<BasePsiNode2<*>> = ArrayList()
    val files = baseDir.children
    for (file in files) {
      val psiFile = psiManager.findFile(file)
      if (psiFile != null) {
        nodes.add(PsiFileNode2(psiFile, settings))
      }

      val psiDir = psiManager.findDirectory(file)
      if (psiDir != null) {
        nodes.add(TutuPsiDirectoryNode(Uni.todoDefaultProject, psiDir, settings))
      }
    }
    return nodes
  }

}
