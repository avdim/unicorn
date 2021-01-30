// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.ViewSettings;

import com.intellij.ide.util.treeView.ValidateableNode;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.StatePreservingNavigatable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.impl.smartPointers.DebugBlackFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.AstLoadingFilter;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from Value.
 *
 * @param <Value> Value of node descriptor
 */
public abstract class AbstractPsiBasedNode2<Value> extends ProjectViewNode2B<Value> implements ValidateableNode, StatePreservingNavigatable {
  private static final Logger LOG = Logger.getInstance(AbstractPsiBasedNode2.class.getName());

  protected AbstractPsiBasedNode2(@NotNull Value value, final ViewSettings viewSettings) {
    super(value, viewSettings);
  }

  @Nullable
  protected abstract PsiElement extractPsiFromValue();

  @Nullable
  protected abstract Collection<AbstractTreeNod2<?>> getChildrenImpl();

  protected abstract void updateImpl(@NotNull PresentationData data);

  @Override
  @NotNull
  public final Collection<? extends AbstractTreeNod2<?>> getChildren() {
    return AstLoadingFilter.disallowTreeLoading(this::doGetChildren);
  }

  @NotNull
  private Collection<? extends AbstractTreeNod2<?>> doGetChildren() {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return new ArrayList<>();
    }
    if (!psiElement.isValid()) {
      LOG.error(new IllegalStateException("Node contains invalid PSI: "
        + "\n" + getClass() + " [" + this + "]"
        + "\n" + psiElement.getClass() + " [" + psiElement + "]"));
      return Collections.emptyList();
    }

    Collection<AbstractTreeNod2<?>> children = getChildrenImpl();
    return children != null ? children : Collections.emptyList();
  }

  @Override
  public boolean isValid() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement != null && psiElement.isValid();
  }

  protected boolean isMarkReadOnly() {
    final AbstractTreeNod2<?> parent = getParent();
    if (parent == null) {
      return false;
    }
    if (parent instanceof AbstractPsiBasedNode2) {
      final PsiElement psiElement = ((AbstractPsiBasedNode2<?>) parent).extractPsiFromValue();
      return psiElement instanceof PsiDirectory;
    }

    final Object parentValue = parent.getValue();
    return parentValue instanceof PsiDirectory || parentValue instanceof Module;
  }


  @Override
  public FileStatus getFileStatus() {
    return computeFileStatus(getVirtualFileForValue());
  }

  protected static FileStatus computeFileStatus(@Nullable VirtualFile virtualFile) {
//  also look at FileStatusProvider and VcsFileStatusProvider
    if (virtualFile != null && virtualFile.getFileSystem() instanceof NonPhysicalFileSystem) {
      return FileStatus.SUPPRESSED;  // do not leak light files via cache
    }
    return FileStatus.NOT_CHANGED;
  }

  @Nullable
  private VirtualFile getVirtualFileForValue() {
    PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return null;
    }
    return PsiUtilCore.getVirtualFile(psiElement);
  }

  // Should be called in atomic action

  @Override
  public void update(@NotNull final PresentationData data) {
    AstLoadingFilter.disallowTreeLoading(() -> doUpdate(data));
  }

  private void doUpdate(@NotNull PresentationData data) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (!validate()) {
        return;
      }

      final PsiElement value = extractPsiFromValue();
      LOG.assertTrue(value.isValid());

      int flags = getIconableFlags();

      try {
        Icon icon = value.getIcon(flags);
        data.setIcon(icon);
      } catch (IndexNotReadyException ignored) {
      }
      data.setPresentableText(myName);

      try {
        if (isDeprecated()) {
          data.setAttributesKey(CodeInsightColors.DEPRECATED_ATTRIBUTES);
        }
      } catch (IndexNotReadyException ignored) {
      }
      updateImpl(data);
      data.setIcon(patchIcon(data.getIcon(true), getVirtualFile()));
    });
  }

  @Iconable.IconFlags
  protected int getIconableFlags() {
    int flags = 0;
    ViewSettings settings = getSettings();
    if (settings instanceof ProjectViewSettings && ((ProjectViewSettings) settings).isShowVisibilityIcons()) {
      flags |= Iconable.ICON_FLAG_VISIBILITY;
    }
    if (isMarkReadOnly()) {
      flags |= Iconable.ICON_FLAG_READ_STATUS;
    }
    return flags;
  }

  @Nullable
  public static Icon patchIcon(@Nullable Icon original, @Nullable VirtualFile file) {
    if (file == null || original == null) return original;

    Icon icon = original;

    if (file.is(VFileProperty.SYMLINK)) {
      icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON);
    }

    return icon;
  }

  protected boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean contains(@NotNull final VirtualFile file) {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null || !psiElement.isValid()) {
      return false;
    }

    final PsiFile containingFile = psiElement.getContainingFile();
    if (containingFile == null) {
      return false;
    }
    final VirtualFile valueFile = containingFile.getVirtualFile();
    return file.equals(valueFile);
  }

  @Nullable
  public NavigationItem getNavigationItem() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement instanceof NavigationItem ? (NavigationItem) psiElement : null;
  }

  @Override
  public void navigate(boolean requestFocus, boolean preserveState) {
    if (canNavigate()) {
      if (requestFocus || preserveState) {
        NavigationUtil.openFileWithPsiElement(extractPsiFromValue(), requestFocus, requestFocus);
      } else {
        getNavigationItem().navigate(false);
      }
    }
  }

  @Override
  public void navigate(boolean requestFocus) {
    navigate(requestFocus, false);
  }

  @Override
  public boolean canNavigate() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigateToSource();
  }

  public boolean validate() {
    final PsiElement psiElement = extractPsiFromValue();
    DebugBlackFile.doDebug(getEqualityObject(), psiElement);
    if (psiElement == null || !psiElement.isValid()) {
      setValue(null);
    }

    return getValue() != null;
  }
}
