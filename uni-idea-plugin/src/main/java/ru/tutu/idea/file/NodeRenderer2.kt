// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.ItemPresentation
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
    val p0 = getPresentation(node)
    if (p0 is PresentationData) {
      val presentation = p0
      val color = if (node is NodeDescriptor<*>) node.color else null
      icon = fixIconIfNeeded(presentation.getIcon(false), selected, hasFocus)
      val coloredText = presentation.coloredText
      val forcedForeground = presentation.forcedTextForeground
      if (coloredText.isEmpty()) {
        var text = presentation.presentableText
        if (StringUtil.isEmpty(text)) {
          val valueSting = value.toString()
          text = valueSting
        }
        text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus)
        val simpleTextAttributes = getSimpleTextAttributes(
          presentation, forcedForeground ?: color
        )
        append(text, simpleTextAttributes)
        val location = presentation.locationString
        if (!StringUtil.isEmpty(location)) {
          val attributes =
            SimpleTextAttributes.merge(simpleTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES)
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

  protected fun getPresentation(node: Any?): ItemPresentation? {
    return if (node is PresentableNodeDescriptor<*>) node.presentation else if (node is NavigationItem) node.presentation else null
  }

  protected fun getSimpleTextAttributes(presentation: PresentationData, color: Color?): SimpleTextAttributes {
    val simpleTextAttributes = getSimpleTextAttributes(presentation, scheme)
    return addColorToSimpleTextAttributes(simpleTextAttributes, color)
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

    private fun getSimpleTextAttributes(
      presentation: ItemPresentation?,
      colorsScheme: EditorColorsScheme
    ): SimpleTextAttributes {
      if (presentation is ColoredItemPresentation) {
        val textAttributesKey = presentation.textAttributesKey
          ?: return SimpleTextAttributes.REGULAR_ATTRIBUTES
        val textAttributes = colorsScheme.getAttributes(textAttributesKey)
        return if (textAttributes == null) SimpleTextAttributes.REGULAR_ATTRIBUTES else SimpleTextAttributes.fromTextAttributes(
          textAttributes
        )
      }
      return SimpleTextAttributes.REGULAR_ATTRIBUTES
    }
  }
}