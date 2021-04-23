// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.IdeBundle
import com.intellij.ide.projectView.PresentationData
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2

class PsiFileNode2(value: PsiFile) : BasePsiNode2<PsiFile>(value), NavigatableWithText {
    public override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> = emptyList()

    override fun updateImpl(data: PresentationData) {
        val value = value
        if (value != null) {
            data.presentableText = value.name
            data.setIcon(value.getIcon(Iconable.ICON_FLAG_READ_STATUS))
            val file = getVirtualFile()
            if (file != null && file.`is`(VFileProperty.SYMLINK)) {
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

    override fun navigate(requestFocus: Boolean) {
        super.navigate(requestFocus)
    }

    override fun getNavigateActionText(focusEditor: Boolean): String? {
        return if (isNavigatableLibraryRoot) ActionsBundle.message("action.LibrarySettings.navigate") else null
    }

    override fun getWeight(): Int {
        return 20
    }

    override fun isMarkReadOnly(): Boolean {
        return true
    }

    override fun canRepresent(element: Any): Boolean {
        if (super.canRepresent(element)) return true
        val value = value
        return value != null && element != null && element == value.virtualFile
    }
}