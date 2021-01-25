// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.my.file;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase2;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode2;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractProjectTreeStructure2 extends ProjectAbstractTreeStructureBase2 implements ViewSettings {
  private final AbstractTreeNod2 myRoot;

  public AbstractProjectTreeStructure2(@NotNull Project project) {
    super(project);
    myRoot = createRoot(project, this);
  }

  protected AbstractTreeNod2 createRoot(@NotNull Project project, @NotNull ViewSettings settings) {
    return new ProjectViewProjectNode2(this);
  }

  @NotNull
  @Override
  public final Object getRootElement() {
    return myRoot;
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  @NotNull
  @Override
  public ActionCallback asyncCommit() {
    return asyncCommitDocuments(myProject);
  }

  @Override
  public final boolean hasSomethingToCommit() {
    return !myProject.isDisposed()
           && PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }

  @Override
  public boolean isAlwaysLeaf(@NotNull Object element) {
    if (element instanceof ProjectViewNode) {
      return ((ProjectViewNode)element).isAlwaysLeaf();
    }
    return super.isAlwaysLeaf(element);
  }
}