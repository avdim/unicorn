// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.NavigatableWithText
import com.intellij.ui.SimpleTextAttributes
import com.unicorn.Uni
import com.unicorn.Uni.BOLD_DIRS

abstract class PsiDirectoryNode2(val virtualDir:VirtualFile) : BasePsiNode2(virtualDir), NavigatableWithText {
  override fun updateImpl(data: PresentationData) {
    if (BOLD_DIRS) {
      data.addText(virtualDir.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
    }
  }
  val isFQNameShown: Boolean get() = false
  override fun isValid(): Boolean = true
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

}
