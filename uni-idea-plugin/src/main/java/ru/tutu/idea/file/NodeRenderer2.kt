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

open class NodeRenderer2 : ColoredTreeCellRenderer2() {
  protected fun fixIconIfNeeded(icon: Icon?, selected: Boolean, hasFocus: Boolean): Icon? {
    return if (icon != null && !StartupUiUtil.isUnderDarcula() && Registry.`is`(
        "ide.project.view.change.icon.on.selection",
        true
      ) && selected && hasFocus
    ) {
      IconLoader.getDarkIcon(icon, true)
    } else icon
  }

  override fun customizeCellRenderer(
    tree: JTree,
    value: @NlsSafe Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ) {
    val node = TreeUtil.getUserObject(value)
    if (node is NodeDescriptor<*>) {
      val descriptor = node
      icon = fixIconIfNeeded(descriptor.icon, selected, hasFocus)
    }
    val presentation = when (node) {
      is PresentableNodeDescriptor<*> -> node.presentation
      is NavigationItem -> node.presentation
      else -> null
    }
    if (presentation is PresentationData) {
      val color = if (node is NodeDescriptor<*>) node.color else null
      icon = fixIconIfNeeded(presentation.getIcon(false), selected, hasFocus)
      val coloredText = presentation.coloredText
      val forcedForeground: Color? = presentation.forcedTextForeground
      if (coloredText.isEmpty()) {
        var text = presentation.presentableText
        if (StringUtil.isEmpty(text)) {
          val valueSting = value.toString()
          text = valueSting
        }
        text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus)
        val textAttributesKey = presentation.textAttributesKey
        val simpleTextAttributes = if (textAttributesKey != null) {
          val textAttributes = scheme.getAttributes(textAttributesKey)
          if (textAttributes != null) {
            SimpleTextAttributes.fromTextAttributes(textAttributes)
          } else {
            SimpleTextAttributes.REGULAR_ATTRIBUTES
          }
        } else {
          SimpleTextAttributes.REGULAR_ATTRIBUTES
        }.let {
          addColorToSimpleTextAttributes(it, forcedForeground ?: color)
        }
        append(text, simpleTextAttributes)
        val location: String? = presentation.locationString
        if (!location.isNullOrEmpty()) {
          val attributes = SimpleTextAttributes.merge(simpleTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES)
          append(presentation.locationPrefix + location + presentation.locationSuffix, attributes, false)
        }
      } else {
        var first = true
        var isMain = true
        for (each in coloredText) {
          var simpleTextAttributes = each.attributes
          if (each.attributes.fgColor == null && forcedForeground != null) {
            simpleTextAttributes = addColorToSimpleTextAttributes(each.attributes, forcedForeground)
          }
          if (first) {
            val textAttributesKey = presentation.textAttributesKey
            if (textAttributesKey != null) {
              val forcedAttributes = scheme.getAttributes(textAttributesKey)
              if (forcedAttributes != null) {
                simpleTextAttributes = SimpleTextAttributes.merge(
                  simpleTextAttributes,
                  SimpleTextAttributes.fromTextAttributes(forcedAttributes)
                )
              }
            }
            first = false
          }
          // the first grayed text (inactive foreground, regular or small) ends main speed-searchable text
          isMain = isMain && !Comparing.equal(
            simpleTextAttributes.fgColor,
            SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
          )
          append(each.text, simpleTextAttributes, isMain)
        }
        val location = presentation.locationString
        if (!StringUtil.isEmpty(location)) {
          append(
            presentation.locationPrefix + location + presentation.locationSuffix,
            SimpleTextAttributes.GRAYED_ATTRIBUTES,
            false
          )
        }
      }
      toolTipText = presentation.tooltip
    } else if (value != null) {
      var text: @NlsSafe String? = value.toString()
      if (node is NodeDescriptor<*>) {
        text = node.toString()
      }
      text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus)
      if (text == null) {
        text = ""
      }
      append(text)
      toolTipText = null
    }
  }

  companion object {
    private val scheme: EditorColorsScheme
      private get() = EditorColorsManager.getInstance().schemeForCurrentUITheme

    private fun addColorToSimpleTextAttributes(
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
