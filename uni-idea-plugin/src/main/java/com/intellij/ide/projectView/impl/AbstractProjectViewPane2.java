// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.dnd.DnDSource;
import com.intellij.ide.dnd.DnDTarget;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.impl.FlattenModulesToggleAction;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.RootsProvider;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.tree.TreeUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

import static com.intellij.ide.projectView.impl.HelpersKt.extractValueFromNode;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractProjectViewPane2 {
  public final @NotNull Project myProject;
  @NotNull public DnDAwareTree myTree;
  @NotNull public ProjectAbstractTreeStructureBase myTreeStructure;
  @Nullable public DnDTarget myDropTarget;
  @Nullable public DnDSource myDragSource;

  public AbstractProjectViewPane2(@NotNull Project project) {
    myProject = project;
    Disposable fileManagerDisposable = new Disposable() {
      @Override
      public void dispose() {
        if (myDropTarget != null) {
          DnDManager.getInstance().unregisterTarget(myDropTarget, myTree);
          myDropTarget = null;
        }
        if (myDragSource != null) {
          DnDManager.getInstance().unregisterSource(myDragSource, myTree);
          myDragSource = null;
        }
      }
    };
    Disposer.register(
      Uni.INSTANCE,
      fileManagerDisposable
    );
  }

  public abstract @NotNull String getId();

  public TreePath[] getSelectionPaths() {
    return myTree.getSelectionPaths();
  }

  /**
   * @deprecated added in {@link ProjectViewImpl} automatically
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
  public ToggleAction createFlattenModulesAction(@NotNull BooleanSupplier isApplicable) {
    return new FlattenModulesToggleAction(myProject, () -> isApplicable.getAsBoolean() && ProjectView.getInstance(myProject).isShowModules(getId()),
                                          () -> ProjectView.getInstance(myProject).isFlattenModules(getId()),
                                          value -> ProjectView.getInstance(myProject).setFlattenModules(getId(), value));
  }

  public final TreePath getSelectedPath() {
    return TreeUtil.getSelectedPathIfOne(myTree);
  }

  public @Nullable PsiElement getFirstElementFromNode(@Nullable Object node) {
    return ContainerUtil.getFirstItem(getElementsFromNode(node));
  }

  @NotNull
  public List<PsiElement> getElementsFromNode(@Nullable Object node) {
    Object value = getValueFromNode(node);
    JBIterable<?> it = value instanceof PsiElement || value instanceof VirtualFile ? JBIterable.of(value) :
                       value instanceof Object[] ? JBIterable.of((Object[])value) :
                       value instanceof Iterable ? JBIterable.from((Iterable<?>)value) :
                       JBIterable.of(TreeUtil.getUserObject(node));
    return it.flatten(o -> o instanceof RootsProvider ? ((RootsProvider)o).getRoots() : Collections.singleton(o))
      .map(o -> o instanceof VirtualFile ? PsiUtilCore.findFileSystemItem(myProject, (VirtualFile)o) : o)
      .filter(PsiElement.class)
      .filter(PsiElement::isValid)
      .toList();
  }

  /** @deprecated use {@link AbstractProjectViewPane2#getElementsFromNode(Object)}**/
  @Deprecated
  @Nullable
  public PsiElement getPSIElementFromNode(@Nullable TreeNode node) {
    return getFirstElementFromNode(node);
  }

  @Nullable
  public Module getNodeModule(@Nullable final Object element) {
    if (element instanceof PsiElement) {
      PsiElement psiElement = (PsiElement)element;
      return ModuleUtilCore.findModuleForPsiElement(psiElement);
    }
    return null;
  }

  public final Object @NotNull [] getSelectedElements() {
    TreePath[] paths = getSelectionPaths();
    if (paths == null) return PsiElement.EMPTY_ARRAY;
    ArrayList<Object> list = new ArrayList<>(paths.length);
    for (TreePath path : paths) {
      Object lastPathComponent = path.getLastPathComponent();
      Object element = getValueFromNode(lastPathComponent);
      if (element instanceof Object[]) {
        Collections.addAll(list, (Object[])element);
      }
      else if (element != null) {
        list.add(element);
      }
    }
    return ArrayUtil.toObjectArray(list);
  }

  @Nullable
  public Object getValueFromNode(@Nullable Object node) {
    return extractValueFromNode(node);
  }

  /** @deprecated use {@link AbstractProjectViewPane2#getValueFromNode(Object)} **/
  @Deprecated
  public Object exhumeElementFromNode(DefaultMutableTreeNode node) {
    return getValueFromNode(node);
  }

  public @NotNull TreeExpander createTreeExpander() {
    return new DefaultTreeExpander(this::getTree) {
      public boolean isExpandAllAllowed() {
        JTree tree = getTree();
        TreeModel model = tree.getModel();
        return model == null || model instanceof AsyncTreeModel || model instanceof InvokerSupplier;
      }

      @Override
      public boolean isExpandAllVisible() {
        return isExpandAllAllowed() && Registry.is("ide.project.view.expand.all.action.visible");
      }

      @Override
      public boolean canExpand() {
        return isExpandAllAllowed() && super.canExpand();
      }

      @Override
      public void collapseAll(@NotNull JTree tree, boolean strict, int keepSelectionLevel) {
        super.collapseAll(tree, false, keepSelectionLevel);
      }
    };
  }

  public @NotNull Comparator<NodeDescriptor<?>> createComparator() {
    return new GroupByTypeComparator(myProject, getId());
  }

  void installComparator(AbstractTreeBuilder treeBuilder) {
    installComparator(treeBuilder, createComparator());
  }

  public void installComparator(AbstractTreeBuilder builder, @NotNull Comparator<? super NodeDescriptor<?>> comparator) {
    if (builder != null) {
      builder.setNodeDescriptorComparator(comparator);
    }
  }

  @NotNull public JTree getTree() {
    return myTree;
  }

}
