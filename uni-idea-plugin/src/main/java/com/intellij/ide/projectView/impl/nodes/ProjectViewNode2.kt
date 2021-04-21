// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.RootsProvider;
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

public abstract class ProjectViewNode2<Value> extends AbstractTreeNod2<Value> implements RootsProvider {


  /**
   * Creates an instance of the project view node.
   *
   * @param value        the object (for example, a PSI element) represented by the project view node
   */
  protected ProjectViewNode2(@NotNull Value value) {
    super(value);
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
