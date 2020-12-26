// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.dnd.DnDSource;
import com.intellij.ide.dnd.DnDTarget;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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

  @NotNull public JTree getTree() {
    return myTree;
  }

}
