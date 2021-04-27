// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.nodes.PsiFileNode2
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.ide.projectView.impl.nodes.BasePsiNode2
import com.intellij.ui.SimpleTextAttributes
import com.unicorn.Uni

class ProjectPsiDirectoryNode(
  virtualDir: VirtualFile,
  val openFile: (VirtualFile) -> Unit
) : BasePsiNode2(virtualDir) {

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

  override fun updateImpl(data: PresentationData) {
    if (Uni.BOLD_DIRS) {
      data.addText(virtualFile.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
    }
  }
  val isFQNameShown: Boolean get() = false
  override fun canNavigate(): Boolean = false
  override fun canNavigateToSource(): Boolean = false
  override fun navigate(requestFocus: Boolean) {}

  override fun getWeight(): Int {
    if (Uni.fileManagerConf2.isFoldersAlwaysOnTop) {
      return 20
    }
    return if (isFQNameShown) 70 else 0
  }

  override val isAlwaysShowPlus get(): Boolean = virtualFile.children.isNotEmpty()

}
