@file:Suppress("UnstableApiUsage")//todo remove

package com.intellij.my.file

import com.intellij.history.LocalHistory
import com.intellij.ide.*
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.impl.JavaHelpers
import com.intellij.ide.projectView.impl.SpeedSearchFiles
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.layout.Cell
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.tree.TreeUtil
import com.unicorn.Uni
import com.unicorn.plugin.virtualFile
import java.awt.BorderLayout
import java.awt.Font
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

//@todo.mvi.State(name = "ProjectView", storages = {
//  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
//  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
//})

inline fun <reified T : Any> Array<Any>.filterType() = mapNotNull { it as? T }

fun Cell.uniFiles(
  project: Project,
  rootPaths: List<String>,
  selectionListener: (VirtualFile) -> Unit = {}
) = createUniFilesComponent(project, rootPaths, selectionListener).invoke()

fun createUniFilesComponent(
  project: Project,
  rootPaths: List<String>,
  selectionListener: (VirtualFile) -> Unit = {}
): JComponent = _createUniFilesComponent(project, rootPaths.map { virtualFile(it) }, selectionListener)

@Suppress("DEPRECATION")
private fun _createUniFilesComponent(
  project: Project,
  rootPaths: List<VirtualFile>,
  selectionListener: (VirtualFile) -> Unit
): JComponent {

  @Suppress("NAME_SHADOWING")
  val selectionListener: (VirtualFile) -> Unit = {
    Uni.selectedFile = it
    Uni.log.debug { arrayOf("select file", it) }
    selectionListener(it)
  }

  ApplicationManager.getApplication().assertIsDispatchThread()

  val treeModel = DefaultTreeModel(DefaultMutableTreeNode(null))
  val myTree: ProjectViewTree2 =
    object : ProjectViewTree2(treeModel) {
      override fun toString(): String = "todo title" + " " + super.toString()//todo title
      override fun setFont(font: Font) {
        super.setFont(font.deriveFont(font.size /*+ 3f*/))
      }
    }
  val treeStructure =
    object : AbstractProjectTreeStructure2(), ProjectViewSettings {
      override fun createRoot(): AbstractTreeNod2<*> =
        object : AbstractTreeNod2<VirtualFile>(virtualFile("/tmp")/*todo redundant VirtualFile type*/) {
          override fun getChildren(): Collection<AbstractTreeNod2<*>> = uniFilesRootNodes(project, rootDirs = rootPaths)
          override fun getFileStatus(): FileStatus = FileStatus.NOT_CHANGED
          override fun update(presentation: PresentationData) {
            presentation.setIcon(PlatformIcons.PROJECT_ICON)
            presentation.presentableText = "todo_presentable_text"
          }
          override fun canNavigateToSource(): Boolean = false
          override fun getWeight(): Int = 0
        }

      override fun getChildElements(element: Any): Array<Any> {
        val treeNode = element as AbstractTreeNod2<*>
        val elements = treeNode.getChildren()
        return elements.toTypedArray()
      }

      override fun isShowExcludedFiles(): Boolean = true
      override fun isShowLibraryContents(): Boolean = true
      override fun isUseFileNestingRules(): Boolean = true
    }

  fun getSelectedElements(): Array<Any> = JavaHelpers.pathsToSelectedElements(myTree.selectionPaths)

  val treeBuilder = ProjectTreeBuilder2(myTree, treeModel, treeStructure)
  treeBuilder.setNodeDescriptorComparator(GroupByTypeComparator2())
  fun initTree() {
    myTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    myTree.isRootVisible = false
    myTree.showsRootHandles = true
    myTree.expandPath(TreePath(myTree.model.root))
    EditSourceOnDoubleClickHandler.install(myTree)
    EditSourceOnEnterKeyHandler.install(myTree)
    ToolTipManager.sharedInstance().registerComponent(myTree)
    TreeUtil.installActions(myTree)
    SpeedSearchFiles(myTree)
    myTree.addKeyListener(PsiCopyPasteManager.EscapeHandler())
    CustomizationUtil.installPopupHandler(
      myTree,
      IdeActions.GROUP_PROJECT_VIEW_POPUP,
      ActionPlaces.PROJECT_VIEW_POPUP
    )
  }
  initTree()

  val viewComponent: JComponent = ScrollPaneFactory.createScrollPane(myTree)
  myTree.addSelectionListener {
    selectionListener(it)
  }

  return object : JPanel(), DataProvider {
    private val myCopyPasteDelegator = CopyPasteDelegator(ProjectManager.getInstance().defaultProject, viewComponent)

    init {
      layout = BorderLayout()
      add(viewComponent, BorderLayout.CENTER)
      revalidate()
      repaint()
    }

    override fun getData(dataId: String): Any? {

      if (PlatformDataKeys.PROJECT_CONTEXT.`is`(dataId)) {
        Uni.log.breakPoint("PROJECT_CONTEXT")
      }
      if (PlatformDataKeys.CONTEXT_COMPONENT.`is`(dataId)) {
        Uni.log.breakPoint("CONTEXT_COMPONENT")
      }
      if (PlatformDataKeys.EDITOR.`is`(dataId)) {
        Uni.log.breakPoint("EDITOR")
      }

      if (PlatformDataKeys.TREE_EXPANDER.`is`(dataId)) {
        Uni.log.error{"PlatformDataKeys.TREE_EXPANDER"}
//        return createTreeExpander(myTree)//todo lazy cache
      }

      if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
        // Used for copy/paste multiple files
        val paths = myTree.selectionPaths ?: return null
        val navigatables = ArrayList<Navigatable>()
        for (path in paths) {
          val node = path.lastPathComponent
          val userObject = TreeUtil.getUserObject(node)
          if (userObject is Navigatable) {
            navigatables.add(userObject)
          } else if (node is Navigatable) {
            navigatables.add(node)
          }
        }
        if (navigatables.isEmpty()) {
          return null
        } else {
          return navigatables.toTypedArray()
        }
      }

      if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
        val elements = getSelectedPSIElements(myTree.selectionPaths)
        return if (elements.size == 1) elements[0] else null
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId)) {
        val elements = getSelectedPSIElements(myTree.selectionPaths)
        return if (elements.isEmpty()) null else elements
      }
      if (PlatformDataKeys.CUT_PROVIDER.`is`(dataId)) {
        return myCopyPasteDelegator.cutProvider
      }
      if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) {
        return myCopyPasteDelegator.copyProvider
      }
      if (PlatformDataKeys.PASTE_PROVIDER.`is`(dataId)) {
        return myCopyPasteDelegator.pasteProvider
      }
      if (LangDataKeys.IDE_VIEW.`is`(dataId)) {
        return object : IdeView {
          override fun getOrChooseDirectory(): PsiDirectory? =
            DirectoryChooserUtil.getOrChooseDirectory(this)
          override fun getDirectories(): Array<PsiDirectory> {
//            val directories = ArrayList<PsiDirectory>()
//            for (node in getSelectedNodes(PsiDirectoryNode2::class.java)) {
//              val value = node.value
//              if(value != null) {
//                directories.add(value)
//              }
//            }
//            if (directories.isNotEmpty()) {
//              return directories.toTypedArray()
//            }
            val elements: Array<PsiElement> = getSelectedPSIElements(myTree.selectionPaths)
            if (elements.size == 1) {
              val element = elements[0]
              if (element is PsiDirectory) {
                return arrayOf(element)
              } else if (element is PsiDirectoryContainer) {
                return element.directories
              } else {
                val containingFile = element.containingFile
                if (containingFile != null) {
                  val psiDirectory = containingFile.containingDirectory
                  if (psiDirectory != null) {
                    return arrayOf(psiDirectory)
                  }
                  return PsiDirectory.EMPTY_ARRAY
                }
              }
            }
            return emptyArray()
          }

          override fun selectElement(element: PsiElement) {}
        }
      }
      if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId)) {
        return object : DeleteProvider {
          override fun canDeleteElement(dataContext: DataContext): Boolean {
            return true
          }

          override fun deleteElement(dataContext: DataContext) {
            val validElements: MutableList<PsiElement> = ArrayList()
            for (psiElement in getSelectedPSIElements(myTree.selectionPaths)) {
              if (psiElement.isValid) {
                validElements.add(psiElement)
              }
            }
            val elements = PsiUtilCore.toPsiElementArray(validElements)
            val a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"))
            try {
              DeleteHandler2.deletePsiElement(elements)
            } finally {
              a.finish()
            }
          }
        }
      }
      if (PlatformDataKeys.SELECTED_ITEMS.`is`(dataId)) {
        return getSelectedElements()
      }

      return null
    }
  }
}

fun uniFilesRootNodes(project: Project, rootDirs: List<VirtualFile>): Collection<AbstractTreeNod2<*>> =
  rootDirs.map {
    ProjectPsiDirectoryNode(it) { file->
      // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
      if (false) {
        ProjectManager.getInstance().openProjects
      }
      val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(file, Uni.todoDefaultProject)
      if (type == null || !file.isValid) {
        // If not associated IDEA file type, or external application
      } else if (type is INativeFileType) {
        // например *.pdf
        type.openFileInAssociatedApplication(Uni.todoDefaultProject, file)
      } else {
        FileEditorManager.getInstance(project).openFile(file, true, true)
      }
    }
  }

fun getSelectedPSIElements(selectionPaths: Array<TreePath>?): Array<PsiElement> {
  val paths = selectionPaths ?: return emptyArray()
  val result = ArrayList<PsiElement>()
  for (path in paths) {
    result.addAll(JavaHelpers.getElementsFromNode(ProjectManager.getInstance().defaultProject, path.lastPathComponent))
  }
  return PsiUtilCore.toPsiElementArray(result)
}
