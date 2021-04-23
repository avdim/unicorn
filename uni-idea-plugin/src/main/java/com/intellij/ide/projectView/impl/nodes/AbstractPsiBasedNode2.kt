// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.*
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.*
import com.intellij.openapi.util.Iconable.IconFlags
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.pom.StatePreservingNavigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.intellij.psi.impl.smartPointers.DebugBlackFile.doDebug
import com.intellij.ui.LayeredIcon
import com.intellij.util.AstLoadingFilter
import com.intellij.util.PlatformIcons
import com.unicorn.Uni
import com.unicorn.Uni.todoUseOpenedProject
import javax.swing.Icon
import javax.swing.SwingUtilities

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from V.
 *
 * @param <V> V of node descriptor
</V> */
abstract class AbstractPsiBasedNode2<V : Any>(value: V) : AbstractTreeNod2<V>(value),
  ValidateableNode, StatePreservingNavigatable {
  protected abstract fun extractPsiFromValue(): PsiElement?
  protected abstract fun getChildrenImpl(): Collection<AbstractTreeNod2<*>>
  protected abstract fun updateImpl(data: PresentationData)
  override fun getChildren(): Collection<AbstractTreeNod2<*>> {
    return AstLoadingFilter.disallowTreeLoading(ThrowableComputable<Collection<AbstractTreeNod2<*>?>, RuntimeException> { doGetChildren() }) as Collection<AbstractTreeNod2<*>>
  }

  private fun doGetChildren(): Collection<AbstractTreeNod2<*>?> {
    val psiElement = extractPsiFromValue() ?: return ArrayList()
    if (!psiElement.isValid) {
      LOG.error(
        IllegalStateException(
          """
          Node contains invalid PSI: 
          $javaClass [$this]
          ${psiElement.javaClass} [$psiElement]
          """.trimIndent()
        )
      )
      return emptyList()
    }
    return getChildrenImpl()
  }

  protected abstract fun getVirtualFile(): VirtualFile?

  override fun isValid(): Boolean {
    val psiElement = extractPsiFromValue()
    return psiElement != null && psiElement.isValid
  }

  protected open fun isMarkReadOnly(): Boolean {
    val parent = getParent() ?: return false
    if (parent is AbstractPsiBasedNode2<*>) {
      val psiElement = parent.extractPsiFromValue()
      return psiElement is PsiDirectory
    }
    val parentValue = parent.value
    return parentValue is PsiDirectory || parentValue is Module
  }

  public override fun update(data: PresentationData) {
    AstLoadingFilter.disallowTreeLoading<RuntimeException> { doUpdate(data) }
  }

  private fun doUpdate(data: PresentationData) {
    ApplicationManager.getApplication().runReadAction {
      if (!validate()) {
        return@runReadAction
      }
      val value = extractPsiFromValue()
      LOG.assertTrue(value!!.isValid)
      val flags = iconableFlags
      try {
        val icon = value.getIcon(flags)
        data.setIcon(icon)
      } catch (ignored: IndexNotReadyException) {
      }
      data.presentableText = myName
      updateImpl(data)
      data.setIcon(patchIcon(data.getIcon(true), getVirtualFile()))
    }
  }

  @get:IconFlags
  protected val iconableFlags: Int
    get() {
      var flags = 0
      if (Uni.fileManagerConf2.isShowVisibilityIcons) {
        flags = flags or Iconable.ICON_FLAG_VISIBILITY
      }
      if (isMarkReadOnly()) {
        flags = flags or Iconable.ICON_FLAG_READ_STATUS
      }
      return flags
    }

  val navigationItem: NavigationItem?
    get() {
      val psiElement = extractPsiFromValue()
      return if (psiElement is NavigationItem) psiElement else null
    }

  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {
    if (canNavigate()) {
      if (requestFocus || preserveState) {
        openFileWithPsiElement(getVirtualFile(), extractPsiFromValue(), requestFocus, requestFocus)
      } else {
        navigationItem?.navigate(false)
      }
    }
  }

  override fun navigate(requestFocus: Boolean) {
    navigate(requestFocus, false)
  }

  override fun canNavigate(): Boolean {
    val item = navigationItem
    return item != null && item.canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    val item = navigationItem
    return item != null && item.canNavigateToSource()
  }

  fun validate(): Boolean {
    val psiElement = extractPsiFromValue()
    doDebug(equalityObject!!, psiElement)
    if (psiElement == null || !psiElement.isValid) {
      value = null
    }
    return value != null
  }

  companion object {
    private val LOG = Logger.getInstance(
      AbstractPsiBasedNode2::class.java.name
    )

    fun patchIcon(original: Icon?, file: VirtualFile?): Icon? {
      if (file == null || original == null) return original
      var icon = original
      if (file.`is`(VFileProperty.SYMLINK)) {
        icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON)
      }
      return icon
    }

    fun getRangeToUnfoldOnNavigation(editor: Editor): TextRange {
      val offset = editor.caretModel.offset
      val line = editor.document.getLineNumber(offset)
      val start = editor.document.getLineStartOffset(line)
      val end = editor.document.getLineEndOffset(line)
      return TextRange(start, end)
    }

    protected fun unfoldCurrentLine(editor: Editor) {
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

    private fun openFileWithPsiElement(
      file: VirtualFile?,
      element: PsiElement?,
      searchForOpen: Boolean,
      requestFocus: Boolean
    ): Boolean {
      var openAsNative = false
      if (element is PsiFile) {
        val virtualFile = element.virtualFile
        if (virtualFile != null) {
          val type = virtualFile.fileType
          openAsNative = type is INativeFileType || type is UnknownFileType
        }
      }
      if (searchForOpen) {
        element!!.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null)
      } else {
        element!!.putUserData(FileEditorManager.USE_CURRENT_WINDOW, true)
      }
      val resultRef = Ref<Boolean>()
      val openAsNativeFinal = openAsNative
      // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
      if (false) {
        val openProjects = ProjectManager.getInstance().openProjects
      }
      val proj = todoUseOpenedProject(element.project)
      CommandProcessor.getInstance().executeCommand(proj, {
        if (openAsNativeFinal || !activatePsiElementIfOpen(proj, element, searchForOpen, requestFocus)) {
          val navigationItem = element as NavigationItem?
          if (!navigationItem!!.canNavigate()) {
            resultRef.set(java.lang.Boolean.FALSE)
          } else {
            navigate2(proj, file!!, requestFocus)
            //            navigationItem.navigate(requestFocus);
            resultRef.set(java.lang.Boolean.TRUE)
          }
        }
      }, "", null)
      if (!resultRef.isNull) return resultRef.get()
      element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null)
      return false
    }

    fun navigate2(project: Project, virtualFile: VirtualFile, requestFocus: Boolean) {
      check(virtualFile.isValid) { "target not valid" }
      if (!virtualFile.isDirectory) {
        if (navigateInEditorOrNativeApp(project, virtualFile, requestFocus)) return
      }
      if (navigateInProjectView(project, virtualFile, requestFocus)) return
      val message = IdeBundle.message(
        "error.files.of.this.type.cannot.be.opened",
        ApplicationNamesInfo.getInstance().productName
      )
      Messages.showErrorDialog(project, message, IdeBundle.message("title.cannot.open.file"))
    }

    private fun navigateInEditorOrNativeApp(project: Project, file: VirtualFile, requestFocus: Boolean): Boolean {
      val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(file, project)
      if (type == null || !file.isValid) return false
      return if (type is INativeFileType) {
        type.openFileInAssociatedApplication(project, file)
      } else navigateInEditor(
        project,
        file,
        requestFocus
      )
    }

    fun navigateInEditor(project: Project, file: VirtualFile, requestFocus: Boolean): Boolean {
      return navigateInRequestedEditor(project, file) || navigateInAnyFileEditor(project, file, requestFocus)
    }

    protected fun navigateInAnyFileEditor(project: Project?, file: VirtualFile?, focusEditor: Boolean): Boolean {
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

    private fun navigateInRequestedEditor(project: Project, file: VirtualFile): Boolean {
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

    protected fun navigateInEditor(project: Project?, e: Editor) {
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

    private fun navigateInProjectView(project: Project, file: VirtualFile, requestFocus: Boolean): Boolean {
      val context: SelectInContext = FileSelectInContext(project, file, null)
      for (target in SelectInManager.getInstance(project).targetList) {
        if (context.selectIn(target, requestFocus)) {
          return true
        }
      }
      return false
    }

    private fun activatePsiElementIfOpen(
      project: Project,
      elt: PsiElement,
      searchForOpen: Boolean,
      requestFocus: Boolean
    ): Boolean {
      var elt = elt
      if (!elt.isValid) return false
      elt = elt.navigationElement
      val file = elt.containingFile
      if (file == null || !file.isValid) return false
      val vFile = file.virtualFile ?: return false
      if (!EditorHistoryManager.getInstance(project).hasBeenOpen(vFile)) return false
      val fem = FileEditorManager.getInstance(project)
      if (!fem.isFileOpen(vFile)) {
        fem.openFile(vFile, requestFocus, searchForOpen)
      }
      val range = elt.textRange ?: return false
      val editors = fem.getEditors(vFile)
      for (editor in editors) {
        if (editor is TextEditor) {
          val text = editor.editor
          val offset = text.caretModel.offset
          if (range.containsOffset(offset)) {
            // select the file
            fem.openFile(vFile, requestFocus, searchForOpen)
            return true
          }
        }
      }
      return false
    }
  }
}