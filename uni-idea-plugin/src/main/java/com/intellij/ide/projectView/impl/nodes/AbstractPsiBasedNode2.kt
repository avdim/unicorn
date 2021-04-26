// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.StatePreservingNavigatable
import com.intellij.util.AstLoadingFilter
import com.intellij.util.IconUtil
import com.unicorn.Uni

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from V.
 *
 * @param <V> V of node descriptor
</V> */
abstract class AbstractPsiBasedNode2<V : VirtualFile>(value: V) : AbstractTreeNod2<V>(value),
  ValidateableNode, StatePreservingNavigatable {
  protected abstract fun getChildrenImpl(): Collection<AbstractTreeNod2<*>>
  protected abstract fun updateImpl(data: PresentationData)
  override fun getChildren(): Collection<AbstractTreeNod2<*>> {
    return AstLoadingFilter.disallowTreeLoading(ThrowableComputable<Collection<AbstractTreeNod2<*>?>, RuntimeException> { getChildrenImpl() }) as Collection<AbstractTreeNod2<*>>
  }

  protected abstract fun getVirtualFile(): VirtualFile

  override fun isValid(): Boolean = true

  public override fun update(presentation: PresentationData) {
    AstLoadingFilter.disallowTreeLoading<RuntimeException> {
      ApplicationManager.getApplication().runReadAction {
        presentation.presentableText = getVirtualFile().name
        presentation.setIcon(IconUtil.getIcon(getVirtualFile(), 0, Uni.todoDefaultProject))
        if (false) {
          presentation.setIcon(patchIcon(presentation.getIcon(true), getVirtualFile()))
        }
        presentation.locationString = "hint"
        updateImpl(presentation)
      }
    }
  }

  final override fun navigate(requestFocus: Boolean) {
    navigate(requestFocus, false)
  }

}
