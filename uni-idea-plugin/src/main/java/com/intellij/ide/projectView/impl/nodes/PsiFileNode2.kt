// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.IdeBundle
import com.intellij.ide.projectView.PresentationData
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.intellij.ui.LayeredIcon
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class PsiFileNode2(
  virtualFile: VirtualFile,
  val openFile: (VirtualFile) -> Unit
) : BasePsiNode2(virtualFile), NavigatableWithText {
  public override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> = emptyList()

  override fun updateImpl(data: PresentationData) {
    val value = value
    if (value != null) {
      val file = getVirtualFile()
      if (file.`is`(VFileProperty.SYMLINK)) {
        val target = file.canonicalPath
        if (target == null) {
          data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES)
          data.tooltip = IdeBundle.message("node.project.view.bad.link")
        } else {
          data.tooltip = FileUtil.toSystemDependentName(target)
        }
      }
    }
  }

  override fun canNavigate(): Boolean = true
  private val isNavigatableLibraryRoot: Boolean get() = false
  override fun canNavigateToSource(): Boolean = true

  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {
    openFile(getVirtualFile())
  }

  override fun getNavigateActionText(focusEditor: Boolean): String? {
    return if (isNavigatableLibraryRoot) ActionsBundle.message("action.LibrarySettings.navigate") else null
  }

  override fun getWeight(): Int = 20
}

fun patchIcon(original: Icon?, file: VirtualFile?): Icon? {
  if (file == null || original == null) return original
  var icon = original
  if (file.`is`(VFileProperty.SYMLINK)) {
    icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON)
  }
  return icon
}
