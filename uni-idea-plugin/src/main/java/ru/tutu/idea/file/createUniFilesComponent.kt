@file:Suppress("UnstableApiUsage")//todo remove

package ru.tutu.idea.file

import com.intellij.history.LocalHistory
import com.intellij.ide.*
import com.intellij.ide.dnd.*
import com.intellij.ide.projectView.*
import com.intellij.ide.projectView.impl.*
import com.intellij.ide.projectView.impl.nodes.*
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.DeleteHandler
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.ide.util.treeView.*
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Trinity
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.layout.Cell
import com.intellij.ui.switcher.QuickActionProvider
import com.intellij.ui.tree.TreePathUtil
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.tree.TreeUtil
import com.unicorn.Uni
import com.unicorn.plugin.virtualFile
import java.awt.*
import java.awt.dnd.DnDConstants
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.tree.*

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
  val treeStructure: ProjectAbstractTreeStructureBase =
    object : ProjectTreeStructure(project, FILES_PANE_ID), ProjectViewSettings {
      override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> =
        object : ProjectViewProjectNode2(settings) {
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

  fun getSelectedElements(): Array<Any> {
    return JavaHelpers.pathsToSelectedElements(myTree.selectionPaths)
  }

  fun getSelectedPath(): TreePath? {
    return TreeUtil.getSelectedPathIfOne(myTree)
  }

  fun <T : NodeDescriptor<*>> getSelectedNodes(nodeClass: Class<T>): List<T> {
    val paths: Array<out TreePath> = myTree.selectionPaths ?: return emptyList()
    val result = ArrayList<T>()
    for (path in paths) {
      val userObject = TreeUtil.getLastUserObject(nodeClass, path)
      if (userObject != null) {
        result.add(userObject)
      }
    }
    return result
  }

  fun getSelectedPSIElements(): Array<PsiElement> {
    val paths = myTree.selectionPaths ?: return emptyArray()
    val result = ArrayList<PsiElement>()
    for (path in paths) {
      result.addAll(JavaHelpers.getElementsFromNode(project, path.lastPathComponent))
    }
    return PsiUtilCore.toPsiElementArray(result)
  }

  class MyDragSource : DnDSource {
    override fun canStartDragging(action: DnDAction, dragOrigin: Point): Boolean {
      if ((action.actionId and DnDConstants.ACTION_COPY_OR_MOVE) == 0) return false
      val elements = getSelectedElements()
      val psiElements = getSelectedPSIElements()
      val dataContext = DataManager.getInstance().getDataContext(myTree)
      return psiElements.isNotEmpty() || canDragElements(elements, dataContext, action.actionId)
    }

    override fun startDragging(action: DnDAction, dragOrigin: Point): DnDDragStartBean {
      val psiElements = getSelectedPSIElements()
      return DnDDragStartBean(object : TransferableWrapper {
        override fun asFileList(): List<File>? {
          return PsiCopyPasteManager.asFileList(psiElements)
        }

        override fun getTreePaths(): Array<TreePath> {
          return myTree.selectionPaths ?: emptyArray()
        }

        override fun getTreeNodes(): Array<TreeNode>? {
          return TreePathUtil.toTreeNodes(*treePaths)
        }

        override fun getPsiElements(): Array<PsiElement> {
          return psiElements
        }
      })
    }

    // copy/paste from com.intellij.ide.dnd.aware.DnDAwareTree.createDragImage
    override fun createDraggedImage(action: DnDAction, dragOrigin: Point, bean: DnDDragStartBean): Pair<Image, Point>? {
      val paths = myTree.selectionPaths ?: return null

      val toRender = ArrayList<Trinity<String, Icon, VirtualFile>>()
      myTree.selectionPaths?.forEach { path ->
        val obj: Any = TreeUtil.getLastUserObject(path)!!
        val component = getTreeCellRendererComponent(
          myTree,
          obj,
          myTree.getRowForPath(path)
        )
        val icon = arrayOfNulls<Icon>(1)
        val text = arrayOfNulls<String>(1)
        ObjectUtils.consumeIfCast(
          component,
          ProjectViewRenderer::class.java
        ) { renderer ->
          icon[0] = renderer.icon
        }
        ObjectUtils.consumeIfCast(
          component,
          SimpleColoredComponent::class.java
        ) { renderer ->
          text[0] = renderer.getCharSequence(true).toString()
        }
        val iconAndText = Pair.create(icon[0], text[0])
        toRender.add(
          Trinity.create(
            iconAndText.second, iconAndText.first,
            PsiCopyPasteManager.asVirtualFile(
              ContainerUtil.getFirstItem(
                JavaHelpers.getElementsFromNode(
                  ProjectManager.getInstance().defaultProject,
                  path.lastPathComponent
                )
              )
            )
          )
        )
      }

      var count = 0
      val panel = JPanel(VerticalFlowLayout(0, 0))
      val maxItemsToShow = if (toRender.size < 20) toRender.size else 10
      for (trinity in toRender) {
        val fileLabel = DragImageLabel(project, myTree, trinity.first, trinity.second, trinity.third)
        panel.add(fileLabel)
        count++
        if (count > maxItemsToShow) {
          panel.add(
            DragImageLabel(
              project,
              myTree,
              IdeBundle.message("label.more.files", paths.size - maxItemsToShow),
              EmptyIcon.ICON_16,
              null
            )
          )
          break
        }
      }
      panel.size = panel.preferredSize
      panel.doLayout()

      val image = ImageUtil.createImage(panel.width, panel.height, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.graphics as Graphics2D
      panel.paint(g2)
      g2.dispose()

      return Pair<Image, Point>(image, Point())
    }

  }

  val headlessEnvironment = ApplicationManager.getApplication().isHeadlessEnvironment
  val myDragSource: DnDSource? =
    if (!headlessEnvironment) {
      MyDragSource().also {
        DnDManager.getInstance().registerSource(it, myTree)
      }
    } else {
      null
    }
  val myDropTarget: DnDTarget? =
    if (!headlessEnvironment) {
      object : ProjectViewDropTarget2(myTree, project) {
        override fun getPsiElement(path: TreePath): PsiElement? {
          return ContainerUtil.getFirstItem(JavaHelpers.getElementsFromNode(project, path.lastPathComponent))
        }

        override fun getModule(element: PsiElement): Module? {
          return ModuleUtilCore.findModuleForPsiElement(element)
        }

        override fun update(event: DnDEvent): Boolean {
          return super.update(event)
        }
      }.also {
        DnDManager.getInstance().registerTarget(it, myTree)
      }
    } else {
      null
    }

  val treeBuilder: BaseProjectTreeBuilder2 =
    object : ProjectTreeBuilder2(myTree, treeModel, treeStructure) {
      override fun createUpdater() = object : AbstractTreeUpdater(this) {
        override fun addSubtreeToUpdateByElement(element: Any): Boolean {
          if (element is PsiDirectory) {
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
    }
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
  val fileManagerDisposable = Disposable {
    if (myDropTarget != null) {
      DnDManager.getInstance().unregisterTarget(myDropTarget, myTree)
    }
    if (myDragSource != null) {
      DnDManager.getInstance().unregisterSource(myDragSource, myTree)
    }
  }
  Disposer.register(Uni, fileManagerDisposable)

  val viewComponent: JComponent = ScrollPaneFactory.createScrollPane(myTree)
  myTree.addSelectionListener {
    selectionListener(it)
  }

  return object : JPanel(), DataProvider {
    private val myCopyPasteDelegator = CopyPasteDelegator(project, viewComponent)

    init {
      layout = BorderLayout()
      add(viewComponent, BorderLayout.CENTER)
      revalidate()
      repaint()
    }

    override fun getData(dataId: String): Any? {
      if (PlatformDataKeys.TREE_EXPANDER.`is`(dataId)) return createTreeExpander(myTree)//todo lazy cache

      val nodes = getSelectedNodes(AbstractTreeNode::class.java)
      val data = treeStructure.getDataFromProviders(nodes, dataId)
      if (data != null) {
        return data
      }

      if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
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

      if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
        val elements = getSelectedPSIElements()
        return if (elements.size == 1) elements[0] else null
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId)) {
        val elements = getSelectedPSIElements()
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
          override fun getDirectories(): Array<PsiDirectory> {
            val directories = ArrayList<PsiDirectory>()
            for (node in getSelectedNodes(PsiDirectoryNode::class.java)) {
              directories.add(node.value)
            }
            if (directories.isNotEmpty()) {
              return directories.toTypedArray()
            }

            val elements: Array<PsiElement> = getSelectedPSIElements()
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
                  val file = containingFile.virtualFile
                  if (file is VirtualFileWindow) {
                    val delegate = (file as VirtualFileWindow).delegate
                    val containingDirectory = containingFile.manager.findFile(delegate)?.containingDirectory
                    if (containingDirectory != null) {
                      return arrayOf(containingDirectory)
                    }
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
            fun getElementsToDelete(): Array<PsiElement> {// if is jar-file root
              val elements = getSelectedPSIElements()
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
        fun getSelectNodeElement(): Any? {
          val descriptor = TreeUtil.getLastUserObject(NodeDescriptor::class.java, getSelectedPath()) ?: return null
          return if (descriptor is AbstractTreeNode<*>) descriptor.value else descriptor.element
        }

        val selected = getSelectNodeElement()
        return selected as? Project
      }
      if (LibraryGroupElement.ARRAY_DATA_KEY.`is`(dataId)) {
        val selectedElements = getSelectedElements().filterType<LibraryGroupElement>()
        return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
      }
      if (NamedLibraryElement.ARRAY_DATA_KEY.`is`(dataId)) {
        val selectedElements = getSelectedElements().filterType<NamedLibraryElement>()
        return if (selectedElements.isEmpty()) null else selectedElements.toTypedArray()
      }
      if (PlatformDataKeys.SELECTED_ITEMS.`is`(dataId)) {
        return getSelectedElements()
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
    PsiManager.getInstance(ProjectManager.getInstance().defaultProject).findDirectory(it)
  }.map { psiDirectory: PsiDirectory ->
    TutuPsiDirectoryNode(
      project,
      psiDirectory,
      settings
    )
  }
}
