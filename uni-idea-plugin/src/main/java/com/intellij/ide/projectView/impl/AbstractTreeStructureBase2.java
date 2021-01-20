// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class AbstractTreeStructureBase2 extends AbstractTreeStructure {
  private static final Logger LOG = Logger.getInstance(AbstractTreeStructureBase2.class);
  protected final Project myProject;

  protected AbstractTreeStructureBase2(Project project) {
    myProject = project;
  }

  @Override
  public Object @NotNull [] getChildElements(@NotNull Object element) {
    LOG.assertTrue(element instanceof AbstractTreeNod2, element.getClass().getName());
    AbstractTreeNod2<?> treeNode = (AbstractTreeNod2<?>)element;
    Collection<? extends AbstractTreeNod2<?>> elements = treeNode.getChildren();
    if (elements.contains(null)) {
      LOG.error("node contains null child: " + treeNode + "; " + treeNode.getClass());
    }
    elements.forEach(node -> node.setParent(treeNode));
    return ArrayUtil.toObjectArray(elements);
  }

  @Override
  public boolean isValid(@NotNull Object element) {
    return element instanceof AbstractTreeNod2;
  }

  @Override
  public Object getParentElement(@NotNull Object element) {
    if (element instanceof AbstractTreeNod2){
      return ((AbstractTreeNod2<?>)element).getParent();
    }
    return null;
  }

  @Override
  @NotNull
  public NodeDescriptor<?> createDescriptor(@NotNull final Object element, final NodeDescriptor parentDescriptor) {
    return (NodeDescriptor<?>)element;
  }

  @Nullable
  public Object getDataFromProviders() {
    return null;
  }

}
