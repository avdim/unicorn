// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.util.treeView.AbstractTreeStructureBase;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.List;

public abstract class ProjectAbstractTreeStructureBase2 extends AbstractTreeStructureBase2 {
  private List<TreeStructureProvider> myProviders;

  protected ProjectAbstractTreeStructureBase2(Project project) {
    super(project);
  }

  @Override
  public List<TreeStructureProvider> getProviders() {
    if (myProviders == null) {
      return TreeStructureProvider.EP.getExtensions(myProject);
    }
    return myProviders;
  }

}
