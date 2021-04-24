// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.SmartHashSet
import com.unicorn.Uni
import com.unicorn.Uni.BOLD_DIRS

abstract class PsiDirectoryNode2(val virtualDir:VirtualFile) : BasePsiNode2(virtualDir), NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private val chain: MutableSet<VirtualFile> = SmartHashSet()
  override fun updateImpl(data: PresentationData) {
    val parentValue = parentValue
    synchronized(chain) {
      if (chain.isEmpty()) {
        val ancestor = getVirtualFile(parentValue)
        if (ancestor != null) {
          var file: VirtualFile? = virtualDir
          while (file != null && VfsUtilCore.isAncestor(ancestor, file, true)) {
            chain.add(file)
            file = file.parent
          }
        }
        if (chain.isEmpty()) chain.add(virtualDir)
      }
    }
    if (BOLD_DIRS) {
      data.addText(virtualDir.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
    }
  }

  val isFQNameShown: Boolean
    get() = false

  override fun isValid(): Boolean {
    return true
    //    if (!super.isValid()) return false;
//    return ProjectViewDirectoryHelper.getInstance(getProject())
//      .isValidDirectory(getValue(), getParentValue(), getSettings(), getFilter());
  }

  override fun canNavigate(): Boolean = false
  override fun canNavigateToSource(): Boolean = false
  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {}
  override fun getNavigateActionText(focusEditor: Boolean): String? = "Nav"

  override fun getWeight(): Int {
    if (Uni.fileManagerConf2.isFoldersAlwaysOnTop) {
      return 20
    }
    return if (isFQNameShown) 70 else 0
  }

  override val isAlwaysShowPlus get(): Boolean = getVirtualFile().children.isNotEmpty()

  companion object {
    /**
     * @return a virtual file that identifies the given element
     */
    private fun getVirtualFile(element: Any?): VirtualFile? {
      if (element is PsiDirectory) {
        return element.virtualFile
      }
      return if (element is VirtualFile) element else null
    }
  }
}
