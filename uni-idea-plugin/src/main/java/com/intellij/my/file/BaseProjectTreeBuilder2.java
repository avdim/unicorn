// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.my.file;

import com.intellij.ide.favoritesTreeView.FavoriteTreeNodeDescriptor;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.StatusBarProgress;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

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

  @NotNull
  @Override
  public Promise<Object> revalidateElement(@NotNull Object element) {
    if (!(element instanceof AbstractTreeNod2)) {
      return Promises.rejectedPromise();
    }

    final AsyncPromise<Object> result = new AsyncPromise<>();
    AbstractTreeNod2 node = (AbstractTreeNod2)element;
    final Object value = node.getValue();
    final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(ObjectUtils.tryCast(value, PsiElement.class));
    batch(indicator -> {
      final Ref<Object> target = new Ref<>();
      Promise<Object> callback = _select(element, virtualFile, Conditions.alwaysTrue());
      callback
        .onSuccess(it -> result.setResult(target.get()))
        .onError(e -> result.setError(e));
    });
    return result;
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
  private Promise<Object> _select(Object element,
                                  VirtualFile file,
                                  Condition<? super AbstractTreeNod2<?>> nonStopCondition) {
    AbstractTreeUpdater2 updater = getUpdater();
    if (updater == null) {
      return Promises.rejectedPromise();
    }

    final AsyncPromise<Object> result = new AsyncPromise<>();
//    UiActivityMonitor.getInstance().addActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"), updater.getModalityState());
    batch(indicator -> {
      _select(element, file, true, nonStopCondition, result, indicator, null, false);
//      UiActivityMonitor.getInstance().removeActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"));
    });
    return result;
  }

  private void _select(Object element,
                       VirtualFile file,
                       boolean requestFocus,
                       Condition<? super AbstractTreeNod2<?>> nonStopCondition,
                       AsyncPromise<Object> result,
                       @NotNull final ProgressIndicator indicator,
                       @Nullable final Ref<Object> virtualSelectTarget,
                       boolean isSecondAttempt) {
    AbstractTreeNod2<?> alreadySelected = alreadySelectedNode(element);

    final Runnable onDone = () -> {
      JTree tree = getTree();
      if (tree != null && requestFocus && virtualSelectTarget == null && getUi().isReady()) {
        tree.requestFocus();
      }

      result.setResult(null);
    };

    final Condition<AbstractTreeNod2<?>> condition = abstractTreeNode -> result.getState() == Promise.State.PENDING && nonStopCondition.value(abstractTreeNode);

    if (alreadySelected == null) {
      expandPathTo(file, (AbstractTreeNod2)getTreeStructure().getRootElement(), element, condition, indicator, virtualSelectTarget)
        .onSuccess(node -> {
          if (virtualSelectTarget == null) {
            select(node, onDone);
          }
          else {
            onDone.run();
          }
        })
        .onError(error -> {
          if (isSecondAttempt) {
            result.cancel();
          }
          else {
            _select(file, file, requestFocus, nonStopCondition, result, indicator, virtualSelectTarget, true);
          }
        });
    }
    else if (virtualSelectTarget == null) {
      scrollTo(alreadySelected, onDone);
    }
    else {
      onDone.run();
    }
  }

  private AbstractTreeNod2 alreadySelectedNode(final Object element) {
    final TreePath[] selectionPaths = getTree().getSelectionPaths();
    if (selectionPaths == null || selectionPaths.length == 0) {
      return null;
    }
    for (TreePath selectionPath : selectionPaths) {
      Object selected = selectionPath.getLastPathComponent();
      if (selected instanceof DefaultMutableTreeNode && elementIsEqualTo(selected, element)) {
        Object userObject = ((DefaultMutableTreeNode)selected).getUserObject();
        if (userObject instanceof AbstractTreeNod2) return (AbstractTreeNod2)userObject;
      }
    }
    return null;
  }

  private static boolean elementIsEqualTo(final Object node, final Object element) {
    if (node instanceof DefaultMutableTreeNode) {
      final Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof AbstractTreeNod2) {
        final AbstractTreeNod2 projectViewNode = (AbstractTreeNod2)userObject;
        return projectViewNode.canRepresent(element);
      }
    }
    return false;
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
