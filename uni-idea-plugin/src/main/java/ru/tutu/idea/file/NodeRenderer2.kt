// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Color
import javax.swing.Icon
import javax.swing.JTree

abstract class NodeRenderer2 : ColoredTreeCellRenderer2() {
  protected fun fixIconIfNeeded(icon: Icon?, selected: Boolean, hasFocus: Boolean): Icon? {
    return if (icon != null && !StartupUiUtil.isUnderDarcula() && Registry.`is`(
        "ide.project.view.change.icon.on.selection",
        true
      ) && selected && hasFocus
    ) {
      IconLoader.getDarkIcon(icon, true)
    } else icon
  }

  companion object {
    val scheme: EditorColorsScheme
      get() = EditorColorsManager.getInstance().schemeForCurrentUITheme

    fun addColorToSimpleTextAttributes(
      simpleTextAttributes: SimpleTextAttributes,
      color: Color?
    ): SimpleTextAttributes {
      var simpleTextAttributes = simpleTextAttributes
      if (color != null) {
        val textAttributes = simpleTextAttributes.toTextAttributes()
        textAttributes.foregroundColor = color
        simpleTextAttributes = SimpleTextAttributes.fromTextAttributes(textAttributes)
      }
      return simpleTextAttributes
    }

  }
}
