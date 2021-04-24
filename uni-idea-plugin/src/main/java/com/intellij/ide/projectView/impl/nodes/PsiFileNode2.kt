// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.IdeBundle
import com.intellij.ide.projectView.PresentationData
import com.intellij.idea.ActionsBundle
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2

class PsiFileNode2(val project: Project, value2: PsiFile) : BasePsiNode2(value2.virtualFile), NavigatableWithText {
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

  override fun canNavigate(): Boolean {
    getVirtualFile() //todo check: is file can opened in editor
    return true
  }

  private val isNavigatableLibraryRoot: Boolean
    private get() = false

  fun extractPsiFromValue(): PsiElement? = PsiManager.getInstance(project).findFile(getVirtualFile())
  override fun canNavigateToSource(): Boolean = true

  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {
    if (canNavigate()) {
      openFileWithPsiElement(getVirtualFile(), extractPsiFromValue(), requestFocus, requestFocus)
    }
  }

  override fun getNavigateActionText(focusEditor: Boolean): String? {
    return if (isNavigatableLibraryRoot) ActionsBundle.message("action.LibrarySettings.navigate") else null
  }

  override fun getWeight(): Int {
    return 20
  }

  override fun canRepresent(element: Any): Boolean {
    if (super.canRepresent(element)) return true
    val value = value
    return value != null && element != null && element == getVirtualFile()
  }
}
