package ru.tutu.idea.file

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.ui.UISettings.Companion.instance
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.JBColor
import com.intellij.ui.LoadingNode
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.render.RenderingUtil
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.util.text.JBDateFormat
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.unicorn.Uni
import org.jetbrains.annotations.Nls
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.accessibility.AccessibleContext
import javax.swing.Icon
import javax.swing.JTree

const val NODE_TO_VIRTUAL_FILE = false//todo
val LOADING_NODE_ICON: Icon = JBUIScale.scaleIcon(EmptyIcon.create(8, 16))

fun addColorToSimpleTextAttributes(
  simpleTextAttributes: SimpleTextAttributes,
  color: Color?
): SimpleTextAttributes {
  var result = simpleTextAttributes
  if (color != null) {
    val textAttributes = simpleTextAttributes.toTextAttributes()
    textAttributes.foregroundColor = color
    result = SimpleTextAttributes.fromTextAttributes(textAttributes)
  }
  return result
}

fun fixIconIfNeeded(icon: Icon?, selected: Boolean, hasFocus: Boolean): Icon? {
  return if (icon != null && !StartupUiUtil.isUnderDarcula() && Registry.`is`(
      "ide.project.view.change.icon.on.selection",
      true
    ) && selected && hasFocus
  ) {
    IconLoader.getDarkIcon(icon, true)
  } else icon
}

open class ProjectViewRenderer2 {

  fun getTreeCellRendererComponent(
    myTree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {

    /**
     * Defines whether the tree is selected or not
     */
    var mySelected = false
    /**
     * Defines whether the tree has focus or not
     */
    var myFocused = false
    var myFocusedCalculated = false
    val myUsedCustomSpeedSearchHighlighting = false

    fun isFocused(): Boolean {
      if (!myFocusedCalculated) {
        myFocused = myTree.hasFocus()
        myFocusedCalculated = true
      }
      return myFocused
    }

    val result = object : SimpleColoredComponent() {
      init {
        isOpaque = false
        isIconOpaque = false
        isTransparentIconBackground = true
      }

      override fun getFont(): Font =
        super.getFont() ?: myTree.font ?: Uni.log.fatalError { "front == null" }

      /**
       * When the item is selected then we use default tree's selection foreground.
       * It guaranties readability of selected text in any LAF.
       */
      override fun append(fragment: @Nls String, attributes: SimpleTextAttributes, isMainText: Boolean) {
        if (mySelected && isFocused()) {
          super.append(
            fragment,
            SimpleTextAttributes(attributes.style, UIUtil.getTreeSelectionForeground(true)),
            isMainText
          )
        } else {
          super.append(fragment, attributes, isMainText)
        }
      }

      private inner class AccessibleColoredTreeCellRenderer : AccessibleSimpleColoredComponent()

      override fun getAccessibleContext(): AccessibleContext {
        if (accessibleContext == null) {
          accessibleContext = AccessibleColoredTreeCellRenderer()
        }
        return accessibleContext
      }

      @Suppress("UnstableApiUsage")
      fun rendererComponentInner(
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
      ) {
        clear()
        mySelected = selected
        myFocusedCalculated = false
        // We paint background if and only if tree path is selected and tree has focus.
        // If path is selected and tree is not focused then we just paint focused border.
        setPaintFocusBorder(selected)
        background = if (selected && isFocused()) JBColor(0x3875D6, 0x2F65CA) else null
        foreground = RenderingUtil.getForeground(myTree)
        icon = if (value is LoadingNode) LOADING_NODE_ICON else null
        super.setOpaque(false)  // avoid erasing Nimbus focus frame
        super.setIconOpaque(false)
        val node = TreeUtil.getUserObject(value)
        if (node is NodeDescriptor<*>) {
          icon = fixIconIfNeeded(node.icon, selected, hasFocus)
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
          val scheme: EditorColorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
          if (coloredText.isEmpty()) {
            var text = presentation.presentableText
            if (StringUtil.isEmpty(text)) {
              val valueSting = value.toString()
              text = valueSting
            }
            text = myTree.convertValueToText(text, selected, expanded, leaf, row, hasFocus)
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
            super.append(text, simpleTextAttributes)
            val location: String? = presentation.locationString
            if (!location.isNullOrEmpty()) {
              val attributes = SimpleTextAttributes.merge(simpleTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES)
              super.append(presentation.locationPrefix + location + presentation.locationSuffix, attributes, false)
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
              super.append(each.text, simpleTextAttributes, isMain)
            }
            val location = presentation.locationString
            if (!StringUtil.isEmpty(location)) {
              super.append(
                presentation.locationPrefix + location + presentation.locationSuffix,
                SimpleTextAttributes.GRAYED_ATTRIBUTES,
                false
              )
            }
          }
          toolTipText = presentation.tooltip
        } else {
          val text: String =
            if (node is NodeDescriptor<*>) {
              node.toString()
            } else {
              value.toString()
            }
          super.append(text)
          toolTipText = null
        }
        if (!myUsedCustomSpeedSearchHighlighting && value !is LoadingNode) {
          val speedSearch = SpeedSearchSupply.getSupply(myTree)
          if (speedSearch != null && !speedSearch.isObjectFilteredOut(value)) {
            SpeedSearchUtil.applySpeedSearchHighlighting(myTree, this, true, selected)
          }
        }

        append(" --", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
        if (NODE_TO_VIRTUAL_FILE && node is ProjectViewNode<*> && instance.showInplaceComments) {
          node.project
          //Additional info from file system
          val parentNode = node.parent
          val content = node.value
          if (content is PsiFileSystemItem || content !is PsiElement || parentNode != null && parentNode.value is PsiDirectory) {
            val virtualFile = node.virtualFile
            val ioFile: Path? =
              if (virtualFile == null || virtualFile.isDirectory || !virtualFile.isInLocalFileSystem) {
                null
              } else {
                virtualFile.toNioPath()
              }
            val fileAttributes: BasicFileAttributes? =
              try {
                if (ioFile == null) null else Files.readAttributes(ioFile, BasicFileAttributes::class.java)
              } catch (ignored: Exception) {
                null
              }
            if (fileAttributes != null) {
              append("  ")
              val attributes = SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
              append(JBDateFormat.getFormatter().formatDateTime(fileAttributes.lastModifiedTime().toMillis()), attributes)
              append(", " + StringUtil.formatFileSize(fileAttributes.size()), attributes)
            }
            val project = node.project
            if (Registry.`is`("show.last.visited.timestamps") && virtualFile != null && project != null) {
              IdeDocumentHistoryImpl.appendTimestamp(project, this, virtualFile)
            }
          }
        }

      }

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
    }

    try {
      result.rendererComponentInner(value, selected, expanded, leaf, row, hasFocus)
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: Exception) {
      Uni.log.error { e }
    }
    return result
  }

}
