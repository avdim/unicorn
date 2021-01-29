// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import com.unicorn.Uni;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
  public boolean contains(@NotNull VirtualFile vFile) {
    return true;
  }

}
