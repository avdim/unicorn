// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.tree.LeafState
import com.unicorn.Uni

abstract class AbstractTreeNod2<V : Any>(val value: V) : NavigationItem, Queryable.Contributor, LeafState.Supplier {
  private val myTemplatePresentation: PresentationData by lazy { PresentationData() }
  private val myUpdatedPresentation: PresentationData by lazy { PresentationData() }
  var index = -1
  var childrenSortingStamp: Long = -1
  var updateCount: Long = 0
  var isWasDeclaredAlwaysLeaf = false

  abstract fun getChildren(): Collection<AbstractTreeNod2<*>>
  protected fun postprocess(presentation: PresentationData) {
    setForcedForeground(presentation)
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

  override fun getLeafState(): LeafState = if (isAlwaysShowPlus) LeafState.NEVER else LeafState.DEFAULT
  open val isAlwaysShowPlus: Boolean get() = false
  val element: AbstractTreeNod2<V> get() = this

  override fun equals(other: Any?): Boolean =
    if (other is AbstractTreeNod2<*>) {
      if (javaClass == other.javaClass) {
        value == other.value
      } else {
        false
      }
    } else {
      false
    }

  override fun hashCode(): Int = value.hashCode()
  override fun apply(info: Map<String, String>) {}
  abstract fun getFileStatus(): FileStatus
  override fun navigate(requestFocus: Boolean) {}
  override fun canNavigate(): Boolean = false
  fun canRepresent(element: Any): Boolean = Uni.todoCanRepresentAlwaysTrue()
  override fun getPresentation(): PresentationData = myUpdatedPresentation
  override fun toString(): String = name ?: ""
  abstract fun getWeight(): Int
  protected abstract fun update(presentation: PresentationData)

  fun update(): Boolean {
    val before = presentation.clone()
    val updated = updatedPresentation()
    return apply(updated, before)
  }

  private fun apply(presentation: PresentationData, before: PresentationData): Boolean {
    var result = presentation != before
    myUpdatedPresentation.copyFrom(presentation)
    myUpdatedPresentation.applyFrom(myTemplatePresentation)
    result = result or myUpdatedPresentation.isChanged
    myUpdatedPresentation.isChanged = false
    return result
  }

  private fun updatedPresentation(): PresentationData {
    val p = myUpdatedPresentation
    p.clear()
    update(p)
    postprocess(p)
    return p
  }

}
