// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.UIUtil
import com.unicorn.Uni
import org.jetbrains.annotations.Nls
import java.awt.Font
import javax.accessibility.AccessibleContext
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.TreeCellRenderer

/**
 * @author Vladimir Kondratyev
 */
abstract class ColoredTreeCellRenderer2 : SimpleColoredComponent(), TreeCellRenderer {
  /**
   * Defines whether the tree is selected or not
   */
  var mySelected = false

  /**
   * Defines whether the tree has focus or not
   */
  var myFocused = false
  var myFocusedCalculated = false
  var myUsedCustomSpeedSearchHighlighting = false
  var myTree: JTree? = null //todo not nullable
  var myOpaque = true
  val isFocused: Boolean
    get() {
      if (!myFocusedCalculated) {
        myFocused = calcFocusedState()
        myFocusedCalculated = true
      }
      return myFocused
    }

  fun calcFocusedState(): Boolean {
    return myTree!!.hasFocus()
  }

  override fun setOpaque(isOpaque: Boolean) {
    myOpaque = isOpaque
    super.setOpaque(isOpaque)
  }

  override fun getFont(): Font =
    super.getFont() ?: myTree?.font ?: Uni.log.fatalError { "front == null" }

  /**
   * When the item is selected then we use default tree's selection foreground.
   * It guaranties readability of selected text in any LAF.
   */
  override fun append(fragment: @Nls String, attributes: SimpleTextAttributes, isMainText: Boolean) {
    if (mySelected && isFocused) {
      super.append(
        fragment,
        SimpleTextAttributes(attributes.style, UIUtil.getTreeSelectionForeground(true)),
        isMainText
      )
    } else {
      super.append(fragment, attributes, isMainText)
    }
  }

  override fun getAccessibleContext(): AccessibleContext {
    if (accessibleContext == null) {
      accessibleContext = AccessibleColoredTreeCellRenderer()
    }
    return accessibleContext
  }

  private inner class AccessibleColoredTreeCellRenderer : AccessibleSimpleColoredComponent()

  // The following method are overridden for performance reasons.
  // See the Implementation Note for more information.
  // javax.swing.tree.DefaultTreeCellRenderer
  // javax.swing.DefaultListCellRenderer
  override fun validate() {}
  override fun invalidate() {}
  override fun revalidate() {}
  override fun repaint(tm: Long, x: Int, y: Int, width: Int, height: Int) {}
  public override fun firePropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {}
  override fun firePropertyChange(propertyName: String, oldValue: Byte, newValue: Byte) {}
  override fun firePropertyChange(propertyName: String, oldValue: Char, newValue: Char) {}
  override fun firePropertyChange(propertyName: String, oldValue: Short, newValue: Short) {}
  override fun firePropertyChange(propertyName: String, oldValue: Int, newValue: Int) {}
  override fun firePropertyChange(propertyName: String, oldValue: Long, newValue: Long) {}
  override fun firePropertyChange(propertyName: String, oldValue: Float, newValue: Float) {}
  override fun firePropertyChange(propertyName: String, oldValue: Double, newValue: Double) {}
  override fun firePropertyChange(propertyName: String, oldValue: Boolean, newValue: Boolean) {}

  companion object {
    val LOG = Logger.getInstance(
      ColoredTreeCellRenderer2::class.java
    )
    val LOADING_NODE_ICON: Icon = JBUIScale.scaleIcon(EmptyIcon.create(8, 16))
  }
}
