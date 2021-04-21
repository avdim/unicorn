// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractProjectNode2 extends ProjectViewNode2<Project> {
  protected AbstractProjectNode2() {
    super(Uni.getTodoDefaultProject());
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    presentation.setIcon(PlatformIcons.PROJECT_ICON);
    presentation.setPresentableText("todo_presentable_text");
  }

  /**
   * Returns the virtual file represented by this node or one of its children.
   *
   * @return the virtual file instance, or null if the project view node doesn't represent a virtual file.
   */
  @Override
  @Nullable
  public VirtualFile getVirtualFile() {
    return null;
  }

}
