// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.my.file;

import com.intellij.ide.favoritesTreeView.FavoriteTreeNodeDescriptor;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import com.intellij.ide.util.treeView.AbstractTreeBuilder2;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.StatusBarProgress;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated use {@link com.intellij.ui.tree.AsyncTreeModel} and {@link com.intellij.ui.tree.StructureTreeModel} instead.
 */
public abstract class BaseProjectTreeBuilder2 extends AbstractTreeBuilder2 {
//  protected final Project myProject;

  public BaseProjectTreeBuilder2(/*@NotNull Project project,*/
                                 @NotNull JTree tree,
                                 @NotNull DefaultTreeModel treeModel,
                                 @NotNull AbstractProjectTreeStructure2 treeStructure) {
    init(tree, treeModel, treeStructure);
    getUi().setClearOnHideDelay(Registry.intValue("ide.tree.clearOnHideTime"));
//    myProject = project;
  }

  @Override
  public boolean isAlwaysShowPlus(AbstractTreeNod2 nodeDescriptor) {
    return nodeDescriptor != null && nodeDescriptor.isAlwaysShowPlus();
  }

  @Override
  public final void expandNodeChildren(@NotNull final DefaultMutableTreeNode node) {
    final AbstractTreeNod2 userObject = (AbstractTreeNod2)node.getUserObject();
    if (userObject == null) return;
    Object element = userObject.getElement();
    VirtualFile virtualFile = getFileToRefresh(element);
    super.expandNodeChildren(node);
    if (virtualFile != null) {
      virtualFile.refresh(true, false);
    }
  }

  private static VirtualFile getFileToRefresh(Object element) {
    Object object = element;
    if (element instanceof AbstractTreeNod2) {
      object = ((AbstractTreeNod2)element).getValue();
    }

    return object instanceof PsiDirectory
           ? ((PsiDirectory)object).getVirtualFile()
           : object instanceof PsiFile ? ((PsiFile)object).getVirtualFile() : null;
  }

  @NotNull
  private static List<AbstractTreeNod2<?>> collectChildren(@NotNull DefaultMutableTreeNode node) {
    int childCount = node.getChildCount();
    List<AbstractTreeNod2<?>> result = new ArrayList<>(childCount);
    for (int i = 0; i < childCount; i++) {
      TreeNode childAt = node.getChildAt(i);
      DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)childAt;
      if (defaultMutableTreeNode.getUserObject() instanceof AbstractTreeNod2) {
        AbstractTreeNod2 treeNode = (AbstractTreeNod2)defaultMutableTreeNode.getUserObject();
        result.add(treeNode);
      }
      else if (defaultMutableTreeNode.getUserObject() instanceof FavoriteTreeNodeDescriptor) {
        Uni.getLog().error("should not use: instanceof FavoriteTreeNodeDescriptor");
//        AbstractTreeNod2 treeNode = ((FavoriteTreeNodeDescriptor)defaultMutableTreeNode.getUserObject()).getElement();
//        result.add(treeNode);
      }
    }
    return result;
  }

  @NotNull
  private Promise<AbstractTreeNod2<?>> expandPathTo(final VirtualFile file,
                                                    @NotNull final AbstractTreeNod2 root,
                                                    final Object element,
                                                    @NotNull final Condition<AbstractTreeNod2<?>> nonStopCondition,
                                                    @NotNull final ProgressIndicator indicator,
                                                    @Nullable final Ref<Object> target) {
    final AsyncPromise<AbstractTreeNod2<?>> async = new AsyncPromise<>();
    if (root.canRepresent(element)) {
      if (target == null) {
        expand(root, () -> async.setResult(root));
      }
      else {
        target.set(root);
        async.setResult(root);
      }
      return async;
    }

    if (target == null) {
      expand(root, () -> {
        indicator.checkCanceled();

        final DefaultMutableTreeNode rootNode = getNodeForElement(root);
        if (rootNode != null) {
          final List<AbstractTreeNod2<?>> kids = collectChildren(rootNode);
          expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
        }
        else {
          async.cancel();
        }
      });
    }
    else {
      if (indicator.isCanceled()) {
        async.cancel();
      }
      else {
        final DefaultMutableTreeNode rootNode = getNodeForElement(root);
        final ArrayList<AbstractTreeNod2<?>> kids = new ArrayList<>();
        if (rootNode != null && getTree().isExpanded(new TreePath(rootNode.getPath()))) {
          kids.addAll(collectChildren(rootNode));
        }
        else {
          Object[] childElements = getTreeStructure().getChildElements(root);
          for (Object each : childElements) {
            kids.add((AbstractTreeNod2)each);
          }
        }

        yieldToEDT(() -> {
          if (isDisposed()) return;
          expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
        });
      }
    }

    return async;
  }

  private void expandChild(@NotNull final List<? extends AbstractTreeNod2<?>> kids,
                           int i,
                           @NotNull final Condition<AbstractTreeNod2<?>> nonStopCondition,
                           final VirtualFile file,
                           final Object element,
                           @NotNull final AsyncPromise<? super AbstractTreeNod2<?>> async,
                           @NotNull final ProgressIndicator indicator,
                           final Ref<Object> virtualSelectTarget) {
    while (i < kids.size()) {
      final AbstractTreeNod2 eachKid = kids.get(i);
      final boolean[] nodeWasCollapsed = {true};
      final DefaultMutableTreeNode nodeForElement = getNodeForElement(eachKid);
      if (nodeForElement != null) {
        nodeWasCollapsed[0] = getTree().isCollapsed(new TreePath(nodeForElement.getPath()));
      }

      if (nonStopCondition.value(eachKid)) {
        final Promise<AbstractTreeNod2<?>> result = expandPathTo(file, eachKid, element, nonStopCondition, indicator, virtualSelectTarget);
        result.onSuccess(abstractTreeNode -> {
          indicator.checkCanceled();
          async.setResult(abstractTreeNode);
        });

        if (result.getState() == Promise.State.PENDING) {
          final int next = i + 1;
          result.onError(error -> {
            indicator.checkCanceled();

            if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
              collapseChildren(eachKid, null);
            }
            expandChild(kids, next, nonStopCondition, file, element, async, indicator, virtualSelectTarget);
          });
          return;
        }
        else {
          if (result.getState() == Promise.State.REJECTED) {
            indicator.checkCanceled();
            if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
              collapseChildren(eachKid, null);
            }
            i++;
          }
          else {
            return;
          }
        }
      }
      else {
        //filter tells us to stop here (for instance, in case of module nodes)
        break;
      }
    }
    async.cancel();
  }

  @Override
  public boolean validateNode(@NotNull final Object child) {
    if (child instanceof ProjectViewNode) {
      final ProjectViewNode projectViewNode = (ProjectViewNode)child;
      return projectViewNode.validate();
    }
    return true;
  }

  @Override
  @NotNull
  public ProgressIndicator createProgressIndicator() {
    return new StatusBarProgress();
  }
}
