// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.my.file;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import com.intellij.util.ArrayUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class AbstractProjectTreeStructure2 extends AbstractTreeStructure {
  private final AbstractTreeNod2 myRoot;

  public AbstractProjectTreeStructure2() {
    myRoot = createRoot();
  }

  abstract protected AbstractTreeNod2 createRoot(); /*{
    return new ProjectViewProjectNode2();
  }*/

  @Override
  public Object /*@NotNull*/ [] getChildElements(@NotNull Object element) {
    if (!(element instanceof AbstractTreeNod2)) {
      Uni.getLog().error("!(element instanceof AbstractTreeNod2)");
    }
    AbstractTreeNod2<?> treeNode = (AbstractTreeNod2<?>)element;
    Collection<? extends AbstractTreeNod2<?>> elements = treeNode.getChildren();
    if (elements.contains(null)) {
      Uni.getLog().error("node contains null child: " + treeNode + "; " + treeNode.getClass());
    }
    return ArrayUtil.toObjectArray(elements);
  }

  @Override
  public boolean isValid(@NotNull Object element) {
    return element instanceof AbstractTreeNod2;
  }

  @Override
  public Object getParentElement(@NotNull Object element) {
    return null;
  }

  @Override
  @NotNull
  public NodeDescriptor<?> createDescriptor(@NotNull final Object element, final NodeDescriptor parentDescriptor) {
    return (NodeDescriptor<?>)element;
  }

  @NotNull
  @Override
  public final Object getRootElement() {
    return myRoot;
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(Uni.getTodoDefaultProject()).commitAllDocuments();
  }

  @NotNull
  @Override
  public ActionCallback asyncCommit() {
    return asyncCommitDocuments(Uni.getTodoDefaultProject());
  }

  @Override
  public final boolean hasSomethingToCommit() {
    return !Uni.getTodoDefaultProject().isDisposed()
           && PsiDocumentManager.getInstance(Uni.getTodoDefaultProject()).hasUncommitedDocuments();
  }

  @Override
  public boolean isAlwaysLeaf(@NotNull Object element) {
    return false;
  }
}
