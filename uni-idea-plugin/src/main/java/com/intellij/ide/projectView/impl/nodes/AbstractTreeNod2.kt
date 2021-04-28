// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.tree.LeafState
import com.unicorn.Uni

abstract class AbstractTreeNod2<V : Any>(val value: V) : NavigationItem, Queryable.Contributor{
  private val myUpdatedPresentation = PresentationData()
  var index = -1
  var childrenSortingStamp: Long = -1
  var isWasDeclaredAlwaysLeaf = false

  abstract fun getChildren(): Collection<BasePsiNode2>
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

  val sortedName: String get() = name ?: "empty"
  override fun hashCode(): Int = value.hashCode()
  override fun apply(info: Map<String, String>) {}
  override fun navigate(requestFocus: Boolean) {}
  fun canRepresent(element: Any): Boolean = Uni.todoCanRepresentAlwaysTrue()
  override fun getPresentation(): PresentationData = myUpdatedPresentation
  override fun toString(): String = name ?: ""
  protected abstract fun update(presentation: PresentationData)

  fun update(): Boolean {
    val before = presentation.clone()
    myUpdatedPresentation.clear()
    update(myUpdatedPresentation)
    var fgColor = FileStatus.NOT_CHANGED.color
    if (CopyPasteManager.getInstance().isCutElement(value)) {//todo not working
      fgColor = CopyPasteManager.CUT_COLOR
    }
    if (myUpdatedPresentation.forcedTextForeground == null) {
      myUpdatedPresentation.forcedTextForeground = fgColor
    }
    val updated = myUpdatedPresentation
    var result = updated != before
    myUpdatedPresentation.copyFrom(updated)
    myUpdatedPresentation.applyFrom(PresentationData())
    result = result or myUpdatedPresentation.isChanged
    myUpdatedPresentation.isChanged = false
    return result
  }

}
