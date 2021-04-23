// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.impl.nodes.BasePsiNode2
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode2
import com.intellij.ide.projectView.impl.nodes.PsiFileNode2
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.unicorn.Uni

class ProjectPsiDirectoryNode(
  val project: Project,
  value: PsiDirectory
) : PsiDirectoryNode2(value) {

  override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> {
    val baseDir = getVirtualFile()
    val psiManager = PsiManager.getInstance(Uni.todoUseOpenedProject(project))
    val nodes: MutableList<BasePsiNode2<*>> = ArrayList()
    val files = baseDir.children
    for (file in files) {
      val psiFile = psiManager.findFile(file)
      if (psiFile != null) {
        nodes.add(PsiFileNode2(psiFile))
      }

      val psiDir = psiManager.findDirectory(file)
      if (psiDir != null) {
        nodes.add(ProjectPsiDirectoryNode(Uni.todoUseOpenedProject(project), psiDir))
      }
    }
    return nodes
  }

}
