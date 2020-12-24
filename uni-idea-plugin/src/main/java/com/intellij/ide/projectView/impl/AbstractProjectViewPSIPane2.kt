// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.PsiCopyPasteManager;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.ide.util.treeView.*;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractProjectViewPSIPane2 extends AbstractProjectViewPaneMiddle {
  private JScrollPane myComponent;

  protected AbstractProjectViewPSIPane2(@NotNull Project project) {
    super(project);
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    if (myComponent != null) {
      SwingUtilities.updateComponentTreeUI(myComponent);
      return myComponent;
    }
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(null);
    DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    myTree = createTree(treeModel);
    enableDnD();
    myComponent = ScrollPaneFactory.createScrollPane(myTree);
    myTreeStructure = createStructure();
    BaseProjectTreeBuilder treeBuilder = new ProjectTreeBuilder(myProject, myTree, treeModel, null, (ProjectAbstractTreeStructureBase) myTreeStructure) {
      @Override
      protected AbstractTreeUpdater createUpdater() {
        return createTreeUpdater(this);
      }
    };
    installComparator(treeBuilder);
    setTreeBuilder(treeBuilder);
    initTree();
    return myComponent;
  }

  private void initTree() {
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.expandPath(new TreePath(myTree.getModel().getRoot()));
    EditSourceOnDoubleClickHandler.install(myTree);
    EditSourceOnEnterKeyHandler.install(myTree);
    ToolTipManager.sharedInstance().registerComponent(myTree);
    TreeUtil.installActions(myTree);
    new SpeedSearchFiles(myTree);
    myTree.addKeyListener(new PsiCopyPasteManager.EscapeHandler());
    CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_PROJECT_VIEW_POPUP, ActionPlaces.PROJECT_VIEW_POPUP);
  }

  @Override
  public final void dispose() {
    myComponent = null;
    super.dispose();
  }

  @NotNull
  protected abstract ProjectAbstractTreeStructureBase createStructure();

  @NotNull
  protected abstract ProjectViewTree createTree(@NotNull DefaultTreeModel treeModel);

  @NotNull
  protected abstract AbstractTreeUpdater createTreeUpdater(@NotNull AbstractTreeBuilder treeBuilder);

}
