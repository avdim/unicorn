// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.dnd.DnDSource;
import com.intellij.ide.dnd.DnDTarget;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.RootsProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.tree.TreeUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.List;

import static com.intellij.ide.projectView.impl.HelpersKt.getValueFromNode;

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

  public TreePath[] getSelectionPaths() {
    return myTree.getSelectionPaths();
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

  @Nullable
  public Module getNodeModule(@Nullable final Object element) {
    if (element instanceof PsiElement) {
      PsiElement psiElement = (PsiElement)element;
      return ModuleUtilCore.findModuleForPsiElement(psiElement);
    }
    return null;
  }

  @NotNull public JTree getTree() {
    return myTree;
  }

}
