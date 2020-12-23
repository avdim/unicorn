@file:Suppress("UnstableApiUsage")//todo remove

package ru.tutu.idea.file

import com.intellij.history.LocalHistory
import com.intellij.icons.AllIcons
import com.intellij.ide.*
import com.intellij.ide.impl.ProjectPaneSelectInTarget
import com.intellij.ide.projectView.HelpID
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.*
import com.intellij.ide.projectView.impl.nodes.LibraryGroupElement
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElement
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.DeleteHandler
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.layout.Cell
import com.intellij.ui.switcher.QuickActionProvider
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.unicorn.Uni
import com.unicorn.plugin.virtualFile
import java.awt.BorderLayout
import java.awt.Font
import java.util.*
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

//@todo.mvi.State(name = "ProjectView", storages = {
//  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
//  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
//})

inline fun <reified T : Any> Array<Any>.filterType() = mapNotNull { it as? T }
const val FILES_PANE_ID = "TutuProjectPane"

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
  val viewPane = object : AbstractProjectViewPSIPane2(project) {
    val component: JComponent = createComponent().also {
//      UIUtil.removeScrollBorder(it)
      ScrollPaneFactory.createScrollPane(it, false)
    }

    override fun getSelectionPaths(): Array<TreePath>? {
      return super.getSelectionPaths() // ?: emptyArray()
    }

    override fun getTitle(): String = "todo title pane"
    override fun getId(): String = FILES_PANE_ID
    override fun getIcon(): Icon = AllIcons.General.ProjectTab

    override fun createSelectInTarget(): SelectInTarget =
      object : ProjectPaneSelectInTarget(project) {
        override fun getMinorViewId(): String = FILES_PANE_ID//todo не удобно
      }

    override fun createTreeUpdater(treeBuilder: AbstractTreeBuilder): AbstractTreeUpdater/*todo deprecated*/ =
      object : AbstractTreeUpdater(treeBuilder) {
        override fun addSubtreeToUpdateByElement(element: Any): Boolean {
          if (element is PsiDirectory) {
            val treeStructure = treeStructure// as ProjectTreeStructure
            var dirToUpdateFrom: PsiDirectory? = element

            var addedOk: Boolean
            while (!super.addSubtreeToUpdateByElement(dirToUpdateFrom ?: treeStructure.rootElement)
                .also { addedOk = it }
            ) {
              if (dirToUpdateFrom == null) {
                break
              }
              dirToUpdateFrom = dirToUpdateFrom.parentDirectory
            }
            return addedOk
          }
          return super.addSubtreeToUpdateByElement(element)
        }
      }

    override fun createStructure(): ProjectAbstractTreeStructureBase =
      object : ProjectTreeStructure(project, FILES_PANE_ID), ProjectViewSettings {
        override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> =
          object : ProjectViewProjectNode(project, settings) {
            override fun canRepresent(element: Any): Boolean = true
            override fun getChildren(): Collection<AbstractTreeNode<*>> {
              return uniFilesRootNodes(project, settings, rootDirs = rootPaths)
            }
          }

        override fun getChildElements(element: Any): Array<Any> {
          val treeNode = element as AbstractTreeNode<*>
          val elements = treeNode.children
          elements.forEach { it.setParent(treeNode) }
          return elements.toTypedArray()
        }

        override fun isShowExcludedFiles(): Boolean = true
        override fun isShowLibraryContents(): Boolean = true
        override fun isUseFileNestingRules(): Boolean = true
      }

    override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
      return object : ProjectViewTree(treeModel) {
        override fun toString(): String = title + " " + super.toString()
        override fun setFont(font: Font) {
          super.setFont(font.deriveFont(font.size /*+ 3f*/))
        }
      }
    }

    override fun addToolbarActions(actionGroup: DefaultActionGroup) {
      //todo not working
      actionGroup.addAction(object : AnAction("sample action") {
        override fun actionPerformed(e: AnActionEvent) {
          Uni.log.debug { "sample files toolbar action" }
        }
      }) //.setAsSecondary(true)
    }

    // should be first
    override fun getWeight(): Int = 0
//  override fun supportsFlattenModules(): Boolean = false
//  override fun supportsShowExcludedFiles(): Boolean = true
  }

  fun getSelectNodeElement(): Any? {
    val descriptor = TreeUtil.getLastUserObject(NodeDescriptor::class.java, viewPane.selectedPath) ?: return null
    return if (descriptor is AbstractTreeNode<*>) descriptor.value else descriptor.element
  }
  viewPane.tree.addSelectionListener {
    selectionListener(it)
  }
//  viewPane.restoreExpandedPaths()

  return object : JPanel(), DataProvider {
    private val myCopyPasteDelegator = CopyPasteDelegator(project, viewPane.component)

    init {
      layout = BorderLayout()
      add(viewPane.component, BorderLayout.CENTER)
      revalidate()
      repaint()
    }

    override fun getData(dataId: String): Any? {
      val paneSpecificData = viewPane.getData(dataId)
      if (paneSpecificData != null) return paneSpecificData

      if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
        val elements = viewPane.selectedPSIElements
        return if (elements.size == 1) elements[0] else null
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId)) {
        val elements = viewPane.selectedPSIElements
        return if (elements.isEmpty()) null else elements
      }
      if (LangDataKeys.TARGET_PSI_ELEMENT.`is`(dataId)) {
        return null
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
          override fun getDirectories(): Array<PsiDirectory> = viewPane.selectedDirectories
          override fun selectElement(element: PsiElement) {}
        }
      }
      if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId)) {
        return object : DeleteProvider {
          override fun canDeleteElement(dataContext: DataContext): Boolean {
            return true
          }

          override fun deleteElement(dataContext: DataContext) {
            fun getElementsToDelete(): Array<PsiElement> {// if is jar-file root
              val elements = viewPane.selectedPSIElements
              for (idx in elements.indices) {
                val element = elements[idx]
                if (element is PsiDirectory) {
                  val virtualFile = element.virtualFile
                  val path = virtualFile.path
                  if (path.endsWith(JarFileSystem.JAR_SEPARATOR)) { // if is jar-file root
                    val vFile = LocalFileSystem.getInstance().findFileByPath(
                      path.substring(0, path.length - JarFileSystem.JAR_SEPARATOR.length)
                    )
                    if (vFile != null) {
                      val psiFile = PsiManager.getInstance(project).findFile(vFile)
                      if (psiFile != null) {
                        elements[idx] = psiFile
                      }
                    }
                  }
                }
              }
              return elements
            }

            val validElements: MutableList<PsiElement> = ArrayList()
            val elementsToDelete = getElementsToDelete()
            for (psiElement in elementsToDelete) {
              if (psiElement.isValid) {
                validElements.add(psiElement)
              }
            }
            val elements = PsiUtilCore.toPsiElementArray(validElements)
            val a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"))
            try {
              DeleteHandler.deletePsiElement(elements, project)
            } finally {
              a.finish()
            }
          }
        }
      }
      if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
        return HelpID.PROJECT_VIEWS
      }
      if (PlatformDataKeys.PROJECT_CONTEXT.`is`(dataId)) {
        val selected = getSelectNodeElement()
        return selected as? Project
      }
      if (LibraryGroupElement.ARRAY_DATA_KEY.`is`(dataId)) {
        val selectedElements = viewPane.selectedElements.filterType<LibraryGroupElement>()
        return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
      }
      if (NamedLibraryElement.ARRAY_DATA_KEY.`is`(dataId)) {
        val selectedElements = viewPane.selectedElements.filterType<NamedLibraryElement>()
        return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
      }
      if (PlatformDataKeys.SELECTED_ITEMS.`is`(dataId)) {
        return viewPane.selectedElements
      }
      if (QuickActionProvider.KEY.`is`(dataId)) {
        return this
      }

      return null
    }
  }
}

fun uniFilesRootNodes(
  project: Project,
  settings: ViewSettings?,
  rootDirs: List<VirtualFile> = ConfUniFiles.ROOT_DIRS
): Collection<AbstractTreeNode<*>> {
  return rootDirs.mapNotNull {
    PsiManager.getInstance(project).findDirectory(it)
  }.map { psiDirectory: PsiDirectory ->
    TutuPsiDirectoryNode(
      project,
      psiDirectory,
      settings
    )
  }
}
