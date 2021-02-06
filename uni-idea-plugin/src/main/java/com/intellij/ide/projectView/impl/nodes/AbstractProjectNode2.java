// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractProjectNode2 extends ProjectViewNode2<Project> {
  protected AbstractProjectNode2() {
    super(Uni.getTodoDefaultProject());
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    presentation.setIcon(PlatformIcons.PROJECT_ICON);
    presentation.setPresentableText("todo_presentable_text");
  }

  @Override
  public boolean contains() {
    return true;
  }

}
