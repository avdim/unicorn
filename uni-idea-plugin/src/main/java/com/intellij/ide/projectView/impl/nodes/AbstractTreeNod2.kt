// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.RootsProvider
import com.intellij.ide.util.treeView.WeighedItem
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.tree.LeafState
import com.unicorn.Uni
import javax.swing.Icon

abstract class AbstractTreeNod2<V : Any>(value: V) : NavigationItem, Queryable.Contributor, RootsProvider, LeafState.Supplier {
  @JvmField
  protected var myName: @NlsSafe String? = null
  var icon: Icon? = null
  private var myNullValueSet = false
  private var myValue: Any? = null
  private val myNodeWrapper: Boolean = setInternalValue(value)
  private var myTemplatePresentation: PresentationData? = null
  private var myUpdatedPresentation: PresentationData? = null
  var index = -1
  var childrenSortingStamp: Long = -1
  var updateCount: Long = 0
  var isWasDeclaredAlwaysLeaf = false
  abstract fun getChildren(): Collection<AbstractTreeNod2<*>>
  protected fun valueIsCut(): Boolean {
    return CopyPasteManager.getInstance().isCutElement(value)
  }

  protected fun postprocess(presentation: PresentationData) {
    setForcedForeground(presentation)
  }

  private fun setForcedForeground(presentation: PresentationData) {
    val status = getFileStatus()
    var fgColor = status.color
    if (valueIsCut()) {
      fgColor = CopyPasteManager.CUT_COLOR
    }
    if (presentation.forcedTextForeground == null) {
      presentation.forcedTextForeground = fgColor
    }
  }

  protected fun shouldUpdateData(): Boolean {
    return equalityObject != null
  }

  override fun getLeafState(): LeafState {
    return if (isAlwaysShowPlus) LeafState.NEVER else LeafState.DEFAULT
  }

  open val isAlwaysShowPlus: Boolean get() = false
  val element: AbstractTreeNod2<V>? get() = if (equalityObject != null) this as? AbstractTreeNod2<V> else null

  override fun equals(`object`: Any?): Boolean {
    if (`object` === this) return true
    return if (`object` == null || `object`.javaClass != javaClass) false else Comparing.equal(
      myValue,
      (`object` as AbstractTreeNod2<*>).myValue
    )
    // we should not change this behaviour if value is set to null
  }

  override fun hashCode(): Int {
    // we should not change hash code if value is set to null
    val value = myValue
    return value?.hashCode() ?: 0
  }

  var value: V?
    get() {
      val value = equalityObject
      return if (value == null) null else retrieveElement(value) as V?
    }
    set(value) {
      val debug = !myNodeWrapper && LOG.isDebugEnabled
      val hash = if (!debug) 0 else hashCode()
      myNullValueSet = value == null || setInternalValue(value)
      if (debug && hash != hashCode()) {
        LOG.warn("hash code changed: $myValue")
      }
    }

  override fun getRoots(): Collection<VirtualFile> {
    val value = value
    if (value is RootsProvider) {
      return value.roots
    }
    if (value is VirtualFile) {
      return setOf(value)
    }
    if (value is PsiFileSystemItem) {
      val item = value
      return item.virtualFile?.let { setOf(it) } ?: emptySet()
    }
    return emptySet()
  }


  /**
   * Stores the anchor to new value if it is not `null`
   *
   * @param value a new value to set
   * @return `true` if the specified value is `null` and the anchor is not changed
   */
  private fun setInternalValue(value: V): Boolean {
    if (value === TREE_WRAPPER_VALUE) return true
    myValue = value
    return false
  }

  val equalityObject: Any?
    get() = if (myNullValueSet) null else myValue

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
    if (shouldUpdateData()) {
      val before = presentation.clone()
      val updated = updatedPresentation
      return shouldApply() && apply(updated, before)
    }
    return false
  }

  fun applyFrom(desc: AbstractTreeNod2<*>) {
    if (desc is AbstractTreeNod2<*>) {
      apply(desc.presentation)
    } else {
      icon = desc.icon
      myName = desc.myName
    }
  }

  protected fun apply(presentation: PresentationData, before: PresentationData? = null): Boolean {
    icon = presentation.getIcon(false)
    myName = presentation.presentableText
    var updated = presentation != before
    if (myUpdatedPresentation == null) {
      myUpdatedPresentation = createPresentation()
    }
    myUpdatedPresentation!!.copyFrom(presentation)
    if (myTemplatePresentation != null) {
      myUpdatedPresentation!!.applyFrom(myTemplatePresentation)
    }
    updated = updated or myUpdatedPresentation!!.isChanged
    myUpdatedPresentation!!.isChanged = false
    return updated
  }

  private val updatedPresentation: PresentationData
    private get() {
      val presentation = if (myUpdatedPresentation != null) myUpdatedPresentation!! else createPresentation()
      myUpdatedPresentation = presentation
      presentation.clear()
      update(presentation)
      if (shouldPostprocess()) {
        postprocess(presentation)
      }
      return presentation
    }

  protected fun createPresentation(): PresentationData {
    return PresentationData()
  }

  protected open fun shouldPostprocess(): Boolean {
    return true
  }

  protected open fun shouldApply(): Boolean {
    return true
  }

  protected abstract fun update(presentation: PresentationData)
  override fun getPresentation(): PresentationData {
    return if (myUpdatedPresentation == null) templatePresentation else myUpdatedPresentation!!
  }

  protected val templatePresentation: PresentationData
    protected get() {
      if (myTemplatePresentation == null) {
        myTemplatePresentation = createPresentation()
      }
      return myTemplatePresentation!!
    }

  override fun toString(): String {
    // NB!: this method may return null if node is not valid
    // it contradicts the specification, but the fix breaks existing behaviour
    // see com.intellij.ide.util.FileStructurePopup#getSpeedSearchText
    return myName!!
  }

  open fun getWeight(): Int {
    val element: AbstractTreeNod2<*>? = element
    return if (element is WeighedItem) {
      (element as WeighedItem).weight
    } else DEFAULT_WEIGHT
  }

  companion object {
    const val DEFAULT_WEIGHT = 30
    private val LOG = Logger.getInstance(
      AbstractTreeNod2::class.java
    )
    val TREE_WRAPPER_VALUE = Any()
    fun retrieveElement(pointer: Any): Any? {
      return pointer
    }
  }

}