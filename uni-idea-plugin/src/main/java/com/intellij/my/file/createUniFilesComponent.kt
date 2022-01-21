@file:Suppress("UnstableApiUsage")//todo remove

package com.intellij.my.file

import com.intellij.history.LocalHistory
import com.intellij.ide.*
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.impl.JavaHelpers
import com.intellij.ide.projectView.impl.search.SpeedSearchFiles
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.ide.projectView.impl.nodes.BasePsiNode2
import com.intellij.ide.util.treeView.AbstractTreeUi2
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.layout.Cell
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
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

fun Cell.uniFiles(
  project: Project,
  rootPaths: List<String>,
  selectionListener: (VirtualFile) -> Unit = {}
) = createUniFilesComponent(project, rootPaths, selectionListener).invoke()

fun createUniFilesComponent(
  project: Project,
  rootPaths: List<String>,
  selectionListener: (VirtualFile) -> Unit = {}
): JComponent {
  val selectionListener2: (VirtualFile) -> Unit = {
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
        object : AbstractTreeNod2<Unit>(Unit) {
          override fun getChildren(): Collection<BasePsiNode2> = rootPaths.map { virtualFile(it) }.map {
            ProjectPsiDirectoryNode(it) { file ->
              // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
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
          override fun update(presentation: PresentationData) {}
          override fun canNavigate(): Boolean = false

          override fun canNavigateToSource(): Boolean = false
          override fun getName(): String = "todo redundant name"
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

  val treeBuilder = ProjectTreeBuilder2(myTree, treeModel, treeStructure)
  treeBuilder.setNodeDescriptorComparator(GroupByTypeComparator2())//todo inline comparator
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
    selectionListener2(it)
  }
  return filesJPanel(viewComponent, myTree, project)
}

private fun filesJPanel(
  viewComponent: JComponent,
  myTree: ProjectViewTree2,
  project: Project
) = object : JPanel(), DataProvider {
  private val myCopyPasteDelegator = CopyPasteDelegator(ProjectManager.getInstance().defaultProject, viewComponent)

  init {
    layout = BorderLayout()
    add(viewComponent, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  override fun getData(dataId: String): Any? {

    fun getPsiDirectories():Array<PsiDirectory> {
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
      Uni.log.debug { "PlatformDataKeys.TREE_EXPANDER" }
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
      return if (navigatables.isEmpty()) null else navigatables.toTypedArray()
    }

    if (false) {
      if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId)) {
        Uni.log.debug { "CommonDataKeys.CommonDataKeys.VIRTUAL_FILE" }
        val selectionPaths = myTree.selectionPaths
        if (selectionPaths != null) {
          Uni.log.debug { "myTree.selectionPaths: ${myTree.selectionPaths}" }
          val virtualFile =
            (((selectionPaths[0] as? TreePath)?.lastPathComponent as? AbstractTreeUi2.ElementNode)?.userObject as? ProjectPsiDirectoryNode)?.virtualFile
          if (virtualFile != null) {
            Uni.log.debug { "virtualFile: ${virtualFile}" }
            return virtualFile
          }
        }
      }
    }

    if (false) {
      if (LangDataKeys.TARGET_PSI_ELEMENT.`is`(dataId)) {
        Uni.log.debug { "LangDataKeys.TARGET_PSI_ELEMENT" }
        val directories = getPsiDirectories()
        if (directories.isNotEmpty()) {
          return directories[0]
        }
      }
    }

    if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
      val elements = getSelectedPSIElements(myTree.selectionPaths)
      Uni.log.debug { "CommonDataKeys.PSI_ELEMENT, elements: $elements" }
      return if (elements.size == 1) createPsiElementWrapper(elements[0], project) else null
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
        override fun getOrChooseDirectory(): PsiDirectory? = DirectoryChooserUtil.getOrChooseDirectory(this)

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
          return getPsiDirectories()
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
      Uni.log.debug { "PlatformDataKeys.SELECTED_ITEMS, dataId: $dataId, myTree.selectionPaths: ${myTree.selectionPaths}" }
      return JavaHelpers.pathsToSelectedElements(myTree.selectionPaths)
    }

    return null
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

fun createPsiElementWrapper(psiElement: PsiElement, project: Project) =
  if (psiElement is PsiDirectory) {
    PsiDirectoryWrapper(psiElement, project)
  } else {
    psiElement
  }

class PsiDirectoryWrapper(val psiDirectory: PsiDirectory, val project2: Project) : PsiDirectory by psiDirectory {
  override fun getProject(): Project {
    return project2
//    return psiDirectory.getProject()
  }
}
