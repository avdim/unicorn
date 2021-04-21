// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.RootsProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2

/**
 * A node in the project view tree.
 */
abstract class ProjectViewNode2<T:Any> constructor(value: T) : AbstractTreeNod2<T>(value), RootsProvider {
    override fun getRoots(): Collection<VirtualFile> {
        val value = value
        if (value is RootsProvider) {
            return (value as RootsProvider).roots
        }
        if (value is VirtualFile) {
            return setOf(value as VirtualFile)
        }
        if (value is PsiFileSystemItem) {
            val item = value as PsiFileSystemItem
            return getDefaultRootsFor(item.virtualFile)
        }
        return emptySet()
    }

    override fun shouldPostprocess(): Boolean {
        return !isValidating
    }

    override fun shouldApply(): Boolean {
        return !isValidating
    }

    val isValidating: Boolean
        get() = false

    companion object {
        protected fun getDefaultRootsFor(file: VirtualFile?): Collection<VirtualFile> {
            return file?.let { setOf(it) } ?: emptySet()
        }
    }
}
