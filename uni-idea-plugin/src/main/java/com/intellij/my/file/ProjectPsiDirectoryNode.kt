// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode2
import com.intellij.ide.projectView.impl.nodes.PsiFileNode2
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2

class ProjectPsiDirectoryNode(
  virtualDir: VirtualFile,
  val openFile: (VirtualFile) -> Unit
) : PsiDirectoryNode2(virtualDir) {

  override fun getChildren(): Collection<AbstractTreeNod2<*>> {
    val baseDir = virtualFile
    val nodes: MutableList<AbstractTreeNod2<*>> = ArrayList()
    for (file in baseDir.children) {
      if (file.isDirectory) {
        nodes.add(ProjectPsiDirectoryNode(file, openFile))
      } else {
        nodes.add(PsiFileNode2(file, openFile))
      }
    }
    return nodes
  }

}
