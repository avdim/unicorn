// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("UnstableApiUsage")

package com.intellij.ide.projectView.impl

import com.intellij.ide.PsiCopyPasteManager
import com.intellij.ide.projectView.BaseProjectTreeBuilder
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

abstract class AbstractProjectViewPSIPane2 constructor(project: Project) : AbstractProjectViewPane2(project) {

  override fun createComponent(): JComponent {
    val rootNode = DefaultMutableTreeNode(null)
    val treeModel = DefaultTreeModel(rootNode)
    myTree = createTree(treeModel)
    enableDnD()
    val result: JScrollPane = ScrollPaneFactory.createScrollPane(myTree)
    myTreeStructure = createStructure()
    val treeBuilder: BaseProjectTreeBuilder = object : ProjectTreeBuilder(
      myProject,
      myTree,
      treeModel,
      null,
      (myTreeStructure as ProjectAbstractTreeStructureBase)
    ) {
      override fun createUpdater(): AbstractTreeUpdater = createTreeUpdater(this)
    }
    installComparator(treeBuilder)
    setTreeBuilder(treeBuilder)
    initTree()
    return result
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

  protected abstract fun createStructure(): ProjectAbstractTreeStructureBase
  protected abstract fun createTree(treeModel: DefaultTreeModel): ProjectViewTree
  protected abstract fun createTreeUpdater(treeBuilder: AbstractTreeBuilder): AbstractTreeUpdater
}