// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.RootsProvider;
import com.intellij.ide.projectView.SettingsProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A node in the project view tree.
 *
 *
 */

public abstract class ProjectViewNode2B<Value> extends AbstractTreeNod2<Value> implements RootsProvider, SettingsProvider {

  private final ViewSettings mySettings;

  /**
   * Creates an instance of the project view node.
   *
   * @param value        the object (for example, a PSI element) represented by the project view node
   * @param viewSettings the settings of the project view.
   */
  protected ProjectViewNode2B(@NotNull Value value, ViewSettings viewSettings) {
    super(value);
    mySettings = viewSettings;
  }

  /**
   * Checks if this node or one of its children represents the specified virtual file.
   *
   * @param file the file to check for.
   * @return true if the file is found in the subtree, false otherwise.
   */
  public abstract boolean contains(@NotNull VirtualFile file);

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

  @Override
  public final ViewSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getRoots() {
    Value value = getValue();
    if (value instanceof RootsProvider) {
      return ((RootsProvider)value).getRoots();
    }
    if (value instanceof VirtualFile) {
      return Collections.singleton((VirtualFile)value);
    }
    if (value instanceof PsiFileSystemItem) {
      PsiFileSystemItem item = (PsiFileSystemItem)value;
      return getDefaultRootsFor(item.getVirtualFile());
    }
    return Collections.emptySet();
  }

  protected static Collection<VirtualFile> getDefaultRootsFor(@Nullable VirtualFile file) {
    return file != null ? Collections.singleton(file) : Collections.emptySet();
  }

  @Override
  protected boolean hasProblemFileBeneath() {
    return false;
  }

  @NlsContexts.PopupTitle
  public String getTitle() {
    return "todo empty title";
  }

  @Override
  protected boolean shouldPostprocess() {
    return !isValidating();
  }

  @Override
  protected boolean shouldApply() {
    return !isValidating();
  }

  public boolean isValidating() {
    return false;
  }
}
