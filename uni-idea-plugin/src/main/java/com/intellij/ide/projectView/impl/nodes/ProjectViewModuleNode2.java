// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ViewSettings;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectViewModuleNode2 extends AbstractModuleNode2 {

  public ProjectViewModuleNode2(Project project, @NotNull Module value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNod2<?>> getChildren() {
    Module module = getValue();
    if (module == null || module.isDisposed()) {  // module has been disposed
      return Collections.emptyList();
    }

    final List<VirtualFile> contentRoots = ProjectViewDirectoryHelper.getInstance(myProject).getTopLevelModuleRoots(module, getSettings());
    return ProjectViewDirectoryHelper2.getInstance(myProject).createFileAndDirectoryNodes(contentRoots, getSettings());
  }

  @Override
  public int getWeight() {
    return 10;
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 2;
  }
}
