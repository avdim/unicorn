// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode2
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode2
import com.intellij.ide.projectView.impl.nodes.PsiFileNode2
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2

class ProjectPsiDirectoryNode(
  virtualDir2: VirtualFile,
  val openFile: (VirtualFile) -> Unit
) : PsiDirectoryNode2(virtualDir2) {

  override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> {
    val baseDir = getVirtualFile()
    val nodes: MutableList<AbstractPsiBasedNode2<*>> = ArrayList()
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