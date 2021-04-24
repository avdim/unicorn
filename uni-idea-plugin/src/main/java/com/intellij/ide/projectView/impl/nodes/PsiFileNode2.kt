// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.*
import com.intellij.ide.projectView.PresentationData
import com.intellij.idea.ActionsBundle
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.intellij.ui.LayeredIcon
import com.intellij.util.PlatformIcons
import com.unicorn.Uni.todoUseOpenedProject
import javax.swing.Icon
import javax.swing.SwingUtilities

class PsiFileNode2(val project: Project, value2: PsiFile) : BasePsiNode2(value2.virtualFile), NavigatableWithText {
  public override fun getChildrenImpl(): Collection<AbstractTreeNod2<*>> = emptyList()

  override fun updateImpl(data: PresentationData) {
    val value = value
    if (value != null) {
      val file = getVirtualFile()
      if (file.`is`(VFileProperty.SYMLINK)) {
        val target = file.canonicalPath
        if (target == null) {
          data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES)
          data.tooltip = IdeBundle.message("node.project.view.bad.link")
        } else {
          data.tooltip = FileUtil.toSystemDependentName(target)
        }
      }
    }
  }

  override fun canNavigate(): Boolean {
    getVirtualFile() //todo check: is file can opened in editor
    return true
  }

  private val isNavigatableLibraryRoot: Boolean
    private get() = false

  override fun canNavigateToSource(): Boolean = true

  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {
    if (canNavigate()) {
      openFileWithPsiElement(
        project = project,
        file = getVirtualFile(),
        element = PsiManager.getInstance(project).findFile(getVirtualFile())!!,
        searchForOpen = requestFocus,
        requestFocus = requestFocus
      )
    }
  }

  override fun getNavigateActionText(focusEditor: Boolean): String? {
    return if (isNavigatableLibraryRoot) ActionsBundle.message("action.LibrarySettings.navigate") else null
  }

  override fun getWeight(): Int {
    return 20
  }

  override fun canRepresent(element: Any): Boolean {
    if (super.canRepresent(element)) return true
    val value = value
    return value != null && element != null && element == getVirtualFile()
  }
}

fun openFileWithPsiElement(
  project:Project,
  file: VirtualFile,
  element: PsiElement,
  searchForOpen: Boolean,
  requestFocus: Boolean
): Boolean {
  if (searchForOpen) {
    element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null)
  } else {
    element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, true)
  }
  val resultRef = Ref<Boolean>()
  // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
  if (false) {
    val openProjects = ProjectManager.getInstance().openProjects
  }
  CommandProcessor.getInstance().executeCommand(project, {
    if (file.fileType is INativeFileType || file.fileType is UnknownFileType || !activatePsiElementIfOpen(project, file, searchForOpen, requestFocus)) {
      val navigationItem = element as NavigationItem?
      if (!navigationItem!!.canNavigate()) {
        resultRef.set(java.lang.Boolean.FALSE)
      } else {
        check(file.isValid) { "target not valid" }
        val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(file, project)
        val navigateInEditorOrNativeApp = when {
          type == null || !file.isValid -> {
            false
          }
          type is INativeFileType -> {
            type.openFileInAssociatedApplication(project, file)
          }
          else -> navigateInRequestedEditor(project, file) || navigateInAnyFileEditor(project, file, requestFocus)
        }
        if (file.isDirectory || !navigateInEditorOrNativeApp) {
          if (!navigateInProjectView(project, file, requestFocus)) {
            val message = IdeBundle.message(
              "error.files.of.this.type.cannot.be.opened",
              ApplicationNamesInfo.getInstance().productName
            )
            Messages.showErrorDialog(project, message, IdeBundle.message("title.cannot.open.file"))
          }
        }
        //            navigationItem.navigate(requestFocus);
        resultRef.set(java.lang.Boolean.TRUE)
      }
    }
  }, "", null)
  if (!resultRef.isNull) return resultRef.get()
  element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null)
  return false
}

fun navigateInAnyFileEditor(project: Project?, file: VirtualFile?, focusEditor: Boolean): Boolean {
  val fileEditorManager = FileEditorManager.getInstance(project!!)
  val editors = fileEditorManager.openFile(file!!, focusEditor)
  for (editor in editors) {
    if (editor is TextEditor) {
      val e = editor.editor
      fileEditorManager.runWhenLoaded(e) {
        unfoldCurrentLine(e)
        if (focusEditor) {
          IdeFocusManager.getInstance(project).requestFocus(e.contentComponent, true)
          val ancestor = SwingUtilities.getWindowAncestor(e.contentComponent)
          ancestor?.toFront()
        }
      }
    }
  }
  return editors.size > 0
}

fun unfoldCurrentLine(editor: Editor) {
  val allRegions = editor.foldingModel.allFoldRegions
  val range = getRangeToUnfoldOnNavigation(editor)
  editor.foldingModel.runBatchFoldingOperation {
    for (region in allRegions) {
      if (!region.isExpanded && range.intersects(TextRange.create(region))) {
        region.isExpanded = true
      }
    }
  }
}

fun navigateInProjectView(project: Project, file: VirtualFile, requestFocus: Boolean): Boolean {
  val context: SelectInContext = FileSelectInContext(project, file, null)
  for (target in SelectInManager.getInstance(project).targetList) {
    if (context.selectIn(target, requestFocus)) {
      return true
    }
  }
  return false
}

fun navigateInRequestedEditor(project: Project, file: VirtualFile): Boolean {
  val ctx = DataManager.getInstance().dataContext
  val e = OpenFileDescriptor.NAVIGATE_IN_EDITOR.getData(ctx) ?: return false
  if (e.isDisposed) {
    Logger.getInstance(OpenFileDescriptor::class.java)
      .error("Disposed editor returned for NAVIGATE_IN_EDITOR from $ctx")
    return false
  }
  if (!Comparing.equal(FileDocumentManager.getInstance().getFile(e.document), file)) return false
  navigateInEditor(project, e)
  return true
}

fun navigateInEditor(project: Project?, e: Editor) {
  val caretModel = e.caretModel
  val caretMoved = false
  if (caretMoved) {
    e.selectionModel.removeSelection()
    FileEditorManager.getInstance(project!!).runWhenLoaded(e) {
      e.scrollingModel.scrollToCaret(ScrollType.CENTER)
      unfoldCurrentLine(e)
    }
  }
}

fun activatePsiElementIfOpen(
  project: Project,
  vFile: VirtualFile,
  searchForOpen: Boolean,
  requestFocus: Boolean
): Boolean {
//  if (!elt.isValid) return false
//  val file = elt.navigationElement.containingFile
//  if (file == null || !file.isValid) return false
//  val vFile: VirtualFile = file.virtualFile ?: return false
  if (!EditorHistoryManager.getInstance(project).hasBeenOpen(vFile)) return false
  val fem = FileEditorManager.getInstance(project)
  if (!fem.isFileOpen(vFile)) {
    fem.openFile(vFile, requestFocus, searchForOpen)
  }
//  val range = elt.navigationElement.textRange ?: return false
  val editors = fem.getEditors(vFile)
  for (editor in editors) {
    if (editor is TextEditor) {
      val text = editor.editor
      val offset = text.caretModel.offset
      if (false /*range.containsOffset(offset)*/) {
        // select the file
        fem.openFile(vFile, requestFocus, searchForOpen)
        return true
      }
    }
  }
  return false
}

fun getRangeToUnfoldOnNavigation(editor: Editor): TextRange {
  val offset = editor.caretModel.offset
  val line = editor.document.getLineNumber(offset)
  val start = editor.document.getLineStartOffset(line)
  val end = editor.document.getLineEndOffset(line)
  return TextRange(start, end)
}

fun patchIcon(original: Icon?, file: VirtualFile?): Icon? {
  if (file == null || original == null) return original
  var icon = original
  if (file.`is`(VFileProperty.SYMLINK)) {
    icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON)
  }
  return icon
}
