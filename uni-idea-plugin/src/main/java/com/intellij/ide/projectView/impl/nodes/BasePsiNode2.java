/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class BasePsiNode2<T extends PsiElement> extends AbstractPsiBasedNode2<T> {
  @Nullable
  private final VirtualFile myVirtualFile;

  protected BasePsiNode2(Project project, @NotNull T value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
    myVirtualFile = PsiUtilCore.getVirtualFile(value);
  }

  @Override
  public FileStatus getFileStatus() {
    return computeFileStatus(getVirtualFile(), Objects.requireNonNull(getProject()));
  }

  @Override
  @Nullable
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  @Nullable
  protected PsiElement extractPsiFromValue() {
    return getValue();
  }
}