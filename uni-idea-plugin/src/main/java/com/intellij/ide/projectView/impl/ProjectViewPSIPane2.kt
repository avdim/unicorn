// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("UnstableApiUsage")

package com.intellij.ide.projectView.impl


import com.intellij.ide.DataManager
import com.intellij.ide.IdeBundle
import com.intellij.ide.PsiCopyPasteManager
import com.intellij.ide.dnd.*
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.projectView.BaseProjectTreeBuilder
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.JavaHelpers.getElementsFromNode
import com.intellij.ide.projectView.impl.JavaHelpers.pathsToSelectedElements
import com.intellij.ide.projectView.impl.nodes.AbstractModuleNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.treeView.*
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Trinity
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.tree.TreePathUtil
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.tree.TreeUtil
import com.unicorn.Uni
import ru.tutu.idea.file.FILES_PANE_ID
import ru.tutu.idea.file.uniFilesRootNodes
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.dnd.DnDConstants
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.tree.*


class ProjectViewPSIPane2 constructor(val myProject: Project) {

  lateinit var myTree: DnDAwareTree
  lateinit var myTreeStructure: ProjectAbstractTreeStructureBase
  var myDropTarget: DnDTarget? = null
  var myDragSource: DnDSource? = null

  init {
    val fileManagerDisposable = Disposable {
      if (myDropTarget != null) {
        DnDManager.getInstance().unregisterTarget(myDropTarget, myTree)
        myDropTarget = null
      }
      if (myDragSource != null) {
        DnDManager.getInstance().unregisterSource(myDragSource!!, myTree)
        myDragSource = null
      }
    }
    Disposer.register(
      Uni,
      fileManagerDisposable
    )
  }

  fun createComponent(rootPaths: List<VirtualFile>): JComponent {
    val treeModel = DefaultTreeModel(DefaultMutableTreeNode(null))
    val tree: ProjectViewTree =
      object : ProjectViewTree(treeModel) {
        override fun toString(): String = "todo title" + " " + super.toString()//todo title
        override fun setFont(font: Font) {
          super.setFont(font.deriveFont(font.size /*+ 3f*/))
        }
      }
    val treeStructure: ProjectAbstractTreeStructureBase =
      object : ProjectTreeStructure(myProject, FILES_PANE_ID), ProjectViewSettings {
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

    myTree = tree
    enableDnD()
    myTreeStructure = treeStructure
    val treeBuilder: BaseProjectTreeBuilder =
      object : ProjectTreeBuilder(
        myProject,
        myTree,
        treeModel,
        null,
        treeStructure
      ) {
        override fun createUpdater() = createTreeUpdater(this, treeStructure)
      }
    treeBuilder.setNodeDescriptorComparator(GroupByTypeComparator(myProject, FILES_PANE_ID))
    initTree()
    return ScrollPaneFactory.createScrollPane(myTree)
  }

  private fun initTree() {
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

  fun getData(dataId: String): Any? {
    if (PlatformDataKeys.TREE_EXPANDER.`is`(dataId)) return createTreeExpander(myTree)//todo lazy cache

    val nodes = getSelectedNodes(AbstractTreeNode::class.java)
    val data = myTreeStructure.getDataFromProviders(nodes, dataId)
    if (data != null) {
      return data
    }

    if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
      val paths = getSelectionPaths()
      if (paths == null) return null
      val navigatables = ArrayList<Navigatable>()
      for (path in paths) {
        val node = path.getLastPathComponent()
        val userObject = TreeUtil.getUserObject(node)
        if (userObject is Navigatable) {
          navigatables.add(userObject)
        } else if (node is Navigatable) {
          navigatables.add(node)
        }
      }
      return if (navigatables.isEmpty()) null else navigatables.toTypedArray<Navigatable>()
    }
    return null
  }

  fun <T : NodeDescriptor<*>> getSelectedNodes(nodeClass: Class<T>): List<T> {
    val paths: Array<out TreePath> = getSelectionPaths() ?: return emptyList<T>()
    val result = ArrayList<T>()
    for (path in paths) {
      val userObject = TreeUtil.getLastUserObject<T>(nodeClass, path)
      if (userObject != null) {
        result.add(userObject)
      }
    }
    return result
  }

  fun getSelectedDirectories(): Array<PsiDirectory> {
    val directories = ArrayList<PsiDirectory>()
    for (node in getSelectedNodes(PsiDirectoryNode::class.java)) {
      var directory: PsiDirectory? = node.getValue()
      if (directory != null) {
        directories.add(directory)
        val parentValue = node.getParent().getValue()
        if (parentValue is PsiDirectory && Registry.`is`("projectView.choose.directory.on.compacted.middle.packages")) {
          while (true) {
            directory = directory!!.getParentDirectory()
            if (directory == null || directory == parentValue) {
              break
            }
            directories.add(directory)
          }
        }
      }
    }
    if (directories.isNotEmpty()) {
      return directories.toTypedArray()
    }

    val elements = getSelectedPSIElements()
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
    } else {
      val path = getSelectedPath()
      if (path != null) {
        val component = path.lastPathComponent
        if (component is DefaultMutableTreeNode) {
          return getSelectedDirectoriesInAmbiguousCase(component.userObject)
        }
        return getSelectedDirectoriesInAmbiguousCase(component)
      }
    }
    return PsiDirectory.EMPTY_ARRAY
  }

  /**
   * @see TreeUtil.getUserObject
   */
  @Deprecated("AbstractProjectViewPane#getSelectedPath")
  fun getSelectedNode(): DefaultMutableTreeNode? {
    val path = getSelectedPath()
    return if (path == null) null else ObjectUtils.tryCast<DefaultMutableTreeNode>(
      path.getLastPathComponent(),
      DefaultMutableTreeNode::class.java
    )
  }

  fun enableDnD() {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
      myDropTarget = object : ProjectViewDropTarget2(myTree, myProject) {
        protected override fun getPsiElement(path: TreePath): PsiElement? {
          return getFirstElementFromNode(path.getLastPathComponent())
        }

        protected override fun getModule(element: PsiElement): Module? {
          return getNodeModule(element)
        }

        public override fun cleanUpOnLeave() {
          super.cleanUpOnLeave()
        }

        public override fun update(event: DnDEvent): Boolean {
          return super.update(event)
        }
      }
      myDragSource = MyDragSource()
      val dndManager = DnDManager.getInstance()
      dndManager.registerSource(myDragSource!!, myTree)
      dndManager.registerTarget(myDropTarget, myTree)
    }
  }

  fun getSelectedPSIElements(): Array<PsiElement> {
    val paths = getSelectionPaths()
    if (paths == null) return PsiElement.EMPTY_ARRAY
    val result = ArrayList<PsiElement>()
    for (path in paths) {
      result.addAll(getElementsFromNode(myProject, path.lastPathComponent))
    }
    return PsiUtilCore.toPsiElementArray(result)
  }

  fun getSelectedDirectoriesInAmbiguousCase(userObject: Any): Array<PsiDirectory> {
    if (userObject is AbstractModuleNode) {
      val module = userObject.value
      if (module != null && !module.isDisposed) {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val sourceRoots = moduleRootManager.sourceRoots
        val dirs = ArrayList<PsiDirectory>(sourceRoots.size)
        val psiManager = PsiManager.getInstance(myProject)
        for (sourceRoot in sourceRoots) {
          val directory = psiManager.findDirectory(sourceRoot)
          if (directory != null) {
            dirs.add(directory)
          }
        }
        return dirs.toTypedArray()
      }
    } else if (userObject is ProjectViewNode<*>) {
      val file = userObject.virtualFile
      if (file != null && file.isValid && file.isDirectory) {
        val directory = PsiManager.getInstance(myProject).findDirectory(file)
        if (directory != null) {
          return arrayOf(directory)
        }
      }
    }
    return PsiDirectory.EMPTY_ARRAY
  }

  fun getSelectedElements(): Array<Any> {
    return pathsToSelectedElements(getSelectionPaths())
  }

  fun getSelectionPaths(): Array<TreePath>? {
    return myTree.getSelectionPaths()
  }

  fun getSelectedPath(): TreePath? {
    return TreeUtil.getSelectedPathIfOne(myTree)
  }

  fun getFirstElementFromNode(node: Any?): PsiElement? {
    return ContainerUtil.getFirstItem<PsiElement>(getElementsFromNode(myProject, node))
  }

  fun getNodeModule(element: Any?): Module? {
    if (element is PsiElement) {
      val psiElement = element as PsiElement?
      return ModuleUtilCore.findModuleForPsiElement(psiElement!!)
    }
    return null
  }

  inner class MyDragSource : DnDSource {
    public override fun canStartDragging(action: DnDAction, dragOrigin: Point): Boolean {
      if ((action.getActionId() and DnDConstants.ACTION_COPY_OR_MOVE) == 0) return false
      val elements = getSelectedElements()
      val psiElements = getSelectedPSIElements()
      val dataContext = DataManager.getInstance().getDataContext(myTree)
      return psiElements.size > 0 || canDragElements(elements, dataContext, action.getActionId())
    }

    public override fun startDragging(action: DnDAction, dragOrigin: Point): DnDDragStartBean {
      val psiElements = getSelectedPSIElements()
      val paths = getSelectionPaths()
      return DnDDragStartBean(object : TransferableWrapper {
        public override fun asFileList(): List<File>? {
          return PsiCopyPasteManager.asFileList(psiElements)
        }

        public override fun getTreePaths(): Array<TreePath> {
          return paths ?: emptyArray()
        }

        public override fun getTreeNodes(): Array<TreeNode>? {
          return TreePathUtil.toTreeNodes(*getTreePaths())
        }

        public override fun getPsiElements(): Array<PsiElement>? {
          return psiElements
        }
      })
    }

    // copy/paste from com.intellij.ide.dnd.aware.DnDAwareTree.createDragImage
    public override fun createDraggedImage(action: DnDAction, dragOrigin: Point, bean: DnDDragStartBean): Pair<Image, Point>? {
      val paths = getSelectionPaths()
      if (paths == null) return null

      val toRender = ArrayList<Trinity<String, Icon, VirtualFile>>()
      getSelectionPaths()?.forEach { path ->
        val iconAndText = getIconAndText(path)
        toRender.add(
          Trinity.create<String, Icon, VirtualFile>(
            iconAndText.second, iconAndText.first,
            PsiCopyPasteManager.asVirtualFile(getFirstElementFromNode(path.getLastPathComponent()))
          )
        )
      }

      var count = 0
      val panel = JPanel(VerticalFlowLayout(0, 0))
      val maxItemsToShow = if (toRender.size < 20) toRender.size else 10
      for (trinity in toRender) {
        val fileLabel = DragImageLabel(myProject, myTree, trinity.first, trinity.second, trinity.third)
        panel.add(fileLabel)
        count++
        if (count > maxItemsToShow) {
          panel.add(
            DragImageLabel(
              myProject,
              myTree,
              IdeBundle.message("label.more.files", paths.size - maxItemsToShow),
              EmptyIcon.ICON_16,
              null
            )
          )
          break
        }
      }
      panel.setSize(panel.getPreferredSize())
      panel.doLayout()

      val image = ImageUtil.createImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB)
      val g2 = image.getGraphics() as Graphics2D
      panel.paint(g2)
      g2.dispose()

      return Pair<Image, Point>(image, Point())
    }

    fun getIconAndText(path: TreePath): Pair<Icon, String> {
      val `object` = TreeUtil.getLastUserObject(path)
      val component = myTree.getCellRenderer()
        .getTreeCellRendererComponent(myTree, `object`, false, false, true, myTree.getRowForPath(path), false)
      val icon = arrayOfNulls<Icon>(1)
      val text = arrayOfNulls<String>(1)
      ObjectUtils.consumeIfCast<ProjectViewRenderer>(
        component,
        ProjectViewRenderer::class.java,
        { renderer -> icon[0] = renderer.getIcon() })
      ObjectUtils.consumeIfCast<SimpleColoredComponent>(
        component,
        SimpleColoredComponent::class.java,
        { renderer -> text[0] = renderer.getCharSequence(true).toString() })
      return Pair.create<Icon, String>(icon[0], text[0])
    }
  }

}

fun createTreeUpdater(treeBuilder: AbstractTreeBuilder, treeStructure: AbstractTreeStructure): AbstractTreeUpdater =
  object : AbstractTreeUpdater(treeBuilder) {
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
