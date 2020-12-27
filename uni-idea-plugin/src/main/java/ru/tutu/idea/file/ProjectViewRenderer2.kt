package ru.tutu.idea.file

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.ui.UISettings.Companion.instance
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.text.JBDateFormat
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Color
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.JTree

open class ProjectViewRenderer2 : NodeRenderer2() {
  init {
    isOpaque = false
    isIconOpaque = false
    isTransparentIconBackground = true
  }

  override fun customizeCellRenderer(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ) {
    val node = TreeUtil.getUserObject(value)
    if (node is NodeDescriptor<*>) {
      val descriptor = node
      icon = super.fixIconIfNeeded(descriptor.icon, selected, hasFocus)
    }
    val presentation = when (node) {
      is PresentableNodeDescriptor<*> -> node.presentation
      is NavigationItem -> node.presentation
      else -> null
    }
    if (presentation is PresentationData) {
      val color = if (node is NodeDescriptor<*>) node.color else null
      icon = super.fixIconIfNeeded(presentation.getIcon(false), selected, hasFocus)
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
    } else if (value != null) {
      var text: @NlsSafe String? = value.toString()
      if (node is NodeDescriptor<*>) {
        text = node.toString()
      }
      text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus)
      if (text == null) {
        text = ""
      }
      super.append(text)
      toolTipText = null
    }

    val userObject = TreeUtil.getUserObject(value)
    if (userObject is ProjectViewNode<*> && instance.showInplaceComments) {
      appendInplaceComments(userObject)
    }
  }

  fun appendInplaceComments(project: Project?, file: VirtualFile?) {
    val ioFile = if (file == null || file.isDirectory || !file.isInLocalFileSystem) null else file.toNioPath()
    val fileAttributes = try {
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

    if (Registry.`is`("show.last.visited.timestamps") && file != null && project != null) {
      IdeDocumentHistoryImpl.appendTimestamp(project, this, file)
    }
  }

  fun appendInplaceComments(node: ProjectViewNode<*>) {
    val parentNode = node.parent
    val content = node.value
    if (content is PsiFileSystemItem || content !is PsiElement || parentNode != null && parentNode.value is PsiDirectory) {
      appendInplaceComments(node.project, node.virtualFile)
    }
  }
}