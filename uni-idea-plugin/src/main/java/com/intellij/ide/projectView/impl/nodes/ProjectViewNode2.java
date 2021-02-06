// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.RootsProvider;
import com.intellij.ide.projectView.SettingsProvider;
import com.intellij.ide.projectView.ViewSettings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.util.PsiUtilCore;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A node in the project view tree.
 *
 *
 */

public abstract class ProjectViewNode2<Value> extends AbstractTreeNod2<Value> implements RootsProvider, SettingsProvider {

  private static final ViewSettings mySettings = new ViewSettings() {
    @Override
    public boolean isFlattenPackages() {
      return false;
    }

    @Override
    public boolean isShowMembers() {
      return false;
    }

    @Override
    public boolean isHideEmptyMiddlePackages() {
      return false;
    }

    @Override
    public boolean isCompactDirectories() {
      return false;
    }

    @Override
    public boolean isAbbreviatePackageNames() {
      return false;
    }

    @Override
    public boolean isShowLibraryContents() {
      return true;
    }

    @Override
    public boolean isShowModules() {
      return false;
    }

    @Override
    public boolean isFlattenModules() {
      return false;
    }

    @Override
    public boolean isShowURL() {
      return true;
    }
  };

  /**
   * Creates an instance of the project view node.
   *
   * @param value        the object (for example, a PSI element) represented by the project view node
   */
  protected ProjectViewNode2(@NotNull Value value) {
    super(value);
  }

  /**
   * Checks if this node or one of its children represents the specified virtual file.
   *
   * @return true if the file is found in the subtree, false otherwise.
   */
  public abstract boolean contains();

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

  public boolean someChildContainsFile(final VirtualFile file) {
    return someChildContainsFile(file, true);
  }

  public boolean someChildContainsFile(final VirtualFile file, boolean optimizeByCheckingFileRootsFirst) {
    VirtualFile parent = file.getParent();
    if (optimizeByCheckingFileRootsFirst && parent != null) {
      Collection<VirtualFile> roots = getRoots();
      for (VirtualFile eachRoot : roots) {
        if (parent.equals(eachRoot.getParent()) || VfsUtilCore.isAncestor(eachRoot, file, true)) {
          return true;
        }
      }
    } else {
      return true;
    }
    return false;
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
    if (!Registry.is("projectView.showHierarchyErrors")) return false;

    Project project = Uni.getTodoDefaultProject();
    WolfTheProblemSolver wolf = project == null ? null : WolfTheProblemSolver.getInstance(project);
    return wolf != null && wolf.hasProblemFilesBeneath(virtualFile -> {
      Value value;
      return contains()
             // in case of flattened packages, when package node a.b.c contains error file, node a.b might not.
             && ((value = getValue()) instanceof PsiElement && Comparing.equal(PsiUtilCore.getVirtualFile((PsiElement)value), virtualFile) ||
                 someChildContainsFile(virtualFile));
    });
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
