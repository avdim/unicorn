// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PsiFileNode2 extends BasePsiNode2<PsiFile> implements NavigatableWithText {
  public PsiFileNode2(@NotNull PsiFile value, ViewSettings viewSettings) {
    super(value, viewSettings);
  }

  @Override
  public Collection<AbstractTreeNod2<?>> getChildrenImpl() {
    return ContainerUtil.emptyList();//1
  }

  private boolean isArchive() {
    VirtualFile file = getVirtualFile();
    return file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    PsiFile value = getValue();
    if (value != null) {
      data.setPresentableText(value.getName());
      data.setIcon(value.getIcon(Iconable.ICON_FLAG_READ_STATUS));

      VirtualFile file = getVirtualFile();
      if (file != null && file.is(VFileProperty.SYMLINK)) {
        @NlsSafe String target = file.getCanonicalPath();
        if (target == null) {
          data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
          data.setTooltip(IdeBundle.message("node.project.view.bad.link"));
        }
        else {
          data.setTooltip(FileUtil.toSystemDependentName(target));
        }
      }
    }
  }

  @Override
  public boolean canNavigate() {
    getVirtualFile();//todo check: is file can opened in editor
    return true || super.canNavigate();
  }

  private boolean isNavigatableLibraryRoot() {
    return false;
  }

  @Override
  public void navigate(boolean requestFocus) {
    super.navigate(requestFocus);
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    return isNavigatableLibraryRoot() ? ActionsBundle.message("action.LibrarySettings.navigate") : null;
  }

  @Override
  public int getWeight() {
    return 20;
  }

  @Override
  public String getTitle() {
    VirtualFile file = getVirtualFile();
    return file != null ? FileUtil.getLocationRelativeToUserHome(file.getPresentableUrl()) : super.getTitle();
  }

  @Override
  protected boolean isMarkReadOnly() {
    return true;
  }

  @Override
  public boolean canRepresent(final Object element) {
    if (super.canRepresent(element)) return true;

    PsiFile value = getValue();
    return value != null && element != null && element.equals(value.getVirtualFile());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return super.contains(file) || isArchive() && Comparing.equal(VfsUtil.getLocalFile(file), getVirtualFile());
  }
}