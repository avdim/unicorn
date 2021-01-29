// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.*;
import com.intellij.openapi.module.impl.LoadedModuleDescriptionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ProjectViewProjectNode2 extends AbstractProjectNode2 {
  public ProjectViewProjectNode2() {
    super();
  }

  @Override
  public boolean canRepresent(Object element) {
    Project project = getValue();
    return project == element || project != null && element instanceof VirtualFile && element.equals(project.getBaseDir());
  }

  @Override
  public @NotNull Collection<AbstractTreeNod2<?>> getChildren() {
    Project project = myProject;
    if (project == null || project.isDisposed() || project.isDefault()) {
      return Collections.emptyList();
    }

    List<VirtualFile> topLevelContentRoots = ProjectViewDirectoryHelper.getInstance(project).getTopLevelRoots();

    Set<ModuleDescription> modules = new LinkedHashSet<>(topLevelContentRoots.size());
    for (VirtualFile root : topLevelContentRoots) {
      Module module = ModuleUtilCore.findModuleForFile(root, project);
      if (module != null) {
        modules.add(new LoadedModuleDescriptionImpl(module));
      }
      else {
        String unloadedModuleName = ProjectRootsUtil.findUnloadedModuleByContentRoot(root, project);
        if (unloadedModuleName != null) {
          ContainerUtil.addIfNotNull(modules, ModuleManager.getInstance(project).getUnloadedModuleDescription(unloadedModuleName));
        }
      }
    }


    List<AbstractTreeNod2<?>> nodes = new ArrayList<>(modulesAndGroups(modules));

    String baseDirPath = project.getBasePath();
    VirtualFile baseDir = baseDirPath == null ? null : LocalFileSystem.getInstance().findFileByPath(baseDirPath);
    if (baseDir != null) {
      PsiManager psiManager = PsiManager.getInstance(project);
      VirtualFile[] files = baseDir.getChildren();
      ProjectFileIndex projectFileIndex = null;
      for (VirtualFile file : files) {
        if (!file.isDirectory()) {
          if (projectFileIndex == null) {
            projectFileIndex = ProjectFileIndex.SERVICE.getInstance(myProject);
          }
          if (projectFileIndex.getModuleForFile(file, false) == null) {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile != null) {
              nodes.add(new PsiFileNode2(myProject, psiFile, getSettings()));
            }
          }
        }
      }
    }

    if (getSettings().isShowLibraryContents()) {
      nodes.add(new ExternalLibrariesNode2(project, getSettings()));
    }
    return nodes;
  }

  @NotNull
  @Override
  protected AbstractTreeNod2<?> createModuleGroup(@NotNull final Module module) {
    List<VirtualFile> roots = ProjectViewDirectoryHelper.getInstance(myProject).getTopLevelModuleRoots(module, getSettings());
    if (roots.size() == 1) {
      final PsiDirectory psi = PsiManager.getInstance(myProject).findDirectory(roots.get(0));
      if (psi != null) {
        return new PsiDirectoryNode2(myProject, psi, getSettings());
      }
    }

    return new ProjectViewModuleNode2(myProject, module, getSettings());
  }

  @Override
  protected AbstractTreeNod2<?> createUnloadedModuleNode(@NotNull UnloadedModuleDescription moduleDescription) {
    List<VirtualFile> roots = ProjectViewDirectoryHelper.getInstance(myProject).getTopLevelUnloadedModuleRoots(moduleDescription, getSettings());
    if (roots.size() == 1) {
      final PsiDirectory psi = PsiManager.getInstance(myProject).findDirectory(roots.get(0));
      if (psi != null) {
        return new PsiDirectoryNode2(myProject, psi, getSettings());
      }
    }

    return new ProjectViewUnloadedModuleNode2(myProject, moduleDescription, getSettings());
  }

  @NotNull
  @Override
  protected AbstractTreeNod2 createModuleGroupNode(@NotNull final ModuleGroup moduleGroup) {
    return new ProjectViewModuleGroupNode2(myProject, moduleGroup, getSettings());
  }
}
