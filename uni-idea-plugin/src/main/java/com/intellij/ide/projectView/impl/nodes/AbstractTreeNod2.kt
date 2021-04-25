// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.IconUtil
import com.unicorn.Uni

abstract class AbstractTreeNod2<V : VirtualFile>(val value: V) : NavigationItem, Queryable.Contributor, LeafState.Supplier {
  val myName: String = value.name
  private val myTemplatePresentation: PresentationData by lazy { PresentationData() }
  private val myUpdatedPresentation: PresentationData by lazy { PresentationData() }

  var index = -1
  var childrenSortingStamp: Long = -1
  var updateCount: Long = 0
  var isWasDeclaredAlwaysLeaf = false

  abstract fun getChildren(): Collection<AbstractTreeNod2<*>>
  override fun getLeafState(): LeafState {
    return if (isAlwaysShowPlus) LeafState.NEVER else LeafState.DEFAULT
  }

  open val isAlwaysShowPlus: Boolean get() = false
  val element: AbstractTreeNod2<V> get() = this

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    return if (other == null || other.javaClass != javaClass) false else Comparing.equal(value, (other as AbstractTreeNod2<*>).value)
    // we should not change this behaviour if value is set to null
  }

  override fun hashCode(): Int = value.hashCode()
  override fun apply(info: Map<String, String>) {}
  abstract fun getFileStatus(): FileStatus
  override fun getName(): String? {
    return myName
  }

  override fun navigate(requestFocus: Boolean) {}
  override fun canNavigate(): Boolean {
    return false
  }

  fun canRepresent(element: Any): Boolean = Uni.todoCanRepresentAlwaysTrue()

  fun update(): Boolean {
    setForcedForeground(presentation)
    return true
  }

  private fun setForcedForeground(presentation: PresentationData) {
    val status = getFileStatus()
    var fgColor = status.color
    if (CopyPasteManager.getInstance().isCutElement(value)) {//todo not working
      fgColor = CopyPasteManager.CUT_COLOR
    }
    if (presentation.forcedTextForeground == null) {
      presentation.forcedTextForeground = fgColor
    }
  }

  val presentation: PresentationData by lazy {
    val presentation = PresentationData()
    presentation.presentableText = value.name
    presentation.setIcon(IconUtil.getIcon(value, 0, Uni.todoDefaultProject))
    if (false) {
      presentation.setIcon(patchIcon(presentation.getIcon(true), value))
    }
    presentation.locationString = "hint"
    updateImpl(presentation)
    presentation
  }

  override fun getPresentation(): ItemPresentation = presentation

  protected abstract fun updateImpl(data: PresentationData)

  override fun toString(): String {
    return myName
  }

  open fun getWeight(): Int = DEFAULT_WEIGHT

  companion object {
    const val DEFAULT_WEIGHT = 30
  }

}
