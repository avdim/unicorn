// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.ViewSettings;

import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.DirectoryInfo;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.*;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.openapi.project.ProjectUtil.isProjectOrWorkspaceFile;

@SuppressWarnings("UnstableApiUsage")
public class ProjectViewDirectoryHelper2 {
  protected static final Logger LOG = Logger.getInstance(ProjectViewDirectoryHelper2.class);

  private final Project myProject;
  private final DirectoryIndex myIndex;

  public static ProjectViewDirectoryHelper2 getInstance(@NotNull Project project) {
    return new ProjectViewDirectoryHelper2(project);
  }

  public ProjectViewDirectoryHelper2(Project project) {
    myProject = project;
    myIndex = DirectoryIndex.getInstance(project);
  }

  /**
   * @deprecated use {@link ProjectViewDirectoryHelper2 (Project)}
   */
  @Deprecated
  public ProjectViewDirectoryHelper2(Project project, DirectoryIndex index) {
    myProject = project;
    myIndex = index;
  }

  public boolean skipDirectory() {
    return true;
  }

  public boolean isEmptyMiddleDirectory() {
    return false;
  }

  @NotNull
  public Collection<AbstractTreeNod2<?>> getDirectoryChildren(final PsiDirectory psiDirectory,
                                                              final ViewSettings settings,
                                                              final boolean withSubDirectories) {
    return getDirectoryChildren(psiDirectory, settings, withSubDirectories, null);
  }

  @NotNull
  public Collection<AbstractTreeNod2<?>> getDirectoryChildren(PsiDirectory psiDirectory,
                                                              ViewSettings settings,
                                                              boolean withSubDirectories,
                                                              @Nullable PsiFileSystemItemFilter filter) {
    return AbstractTreeUi.calculateYieldingToWriteAction(() -> doGetDirectoryChildren(psiDirectory, settings, withSubDirectories, filter));
  }

  @NotNull
  private Collection<AbstractTreeNod2<?>> doGetDirectoryChildren(PsiDirectory psiDirectory,
                                                                 ViewSettings settings,
                                                                 boolean withSubDirectories,
                                                                 @Nullable PsiFileSystemItemFilter filter) {
    List<AbstractTreeNod2<?>> children = new ArrayList<>();
    Project project = psiDirectory.getProject();
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    Module module = fileIndex.getModuleForFile(psiDirectory.getVirtualFile());
    ModuleFileIndex moduleFileIndex = module == null ? null : ModuleRootManager.getInstance(module).getFileIndex();
    if (!settings.isFlattenPackages() || skipDirectory()) {
      processPsiDirectoryChildren(directoryChildrenInProject(psiDirectory, settings),
                                  children, fileIndex, null, settings, withSubDirectories, filter);
    }
    else { // source directory in "flatten packages" mode
      final PsiDirectory parentDir = psiDirectory.getParentDirectory();
      if (parentDir == null || skipDirectory() && withSubDirectories) {
        addAllSubpackages(children, psiDirectory, moduleFileIndex, settings, filter);
      }
      if (withSubDirectories) {
        PsiDirectory[] subdirs = psiDirectory.getSubdirectories();
        for (PsiDirectory subdir : subdirs) {
          if (!skipDirectory() || filter != null && !filter.shouldShow(subdir)) {
            continue;
          }
          VirtualFile directoryFile = subdir.getVirtualFile();

          if (Registry.is("ide.hide.excluded.files")) {
            if (fileIndex.isExcluded(directoryFile)) continue;
          }
          else {
            if (FileTypeRegistry.getInstance().isFileIgnored(directoryFile)) continue;
          }

          children.add(new PsiDirectoryNode2(project, subdir, settings, filter));
        }
      }
      processPsiDirectoryChildren(psiDirectory.getFiles(), children, fileIndex, moduleFileIndex, settings,
                                  withSubDirectories, filter);
    }
    return children;
  }

  @NotNull
  public List<VirtualFile> getTopLevelRoots() {
    List<VirtualFile> topLevelContentRoots = new ArrayList<>();
    ProjectRootManager prm = ProjectRootManager.getInstance(myProject);

    for (VirtualFile root : prm.getContentRoots()) {
      VirtualFile parent = root.getParent();
      if (!isFileUnderContentRoot(myIndex, parent)) {
        topLevelContentRoots.add(root);
      }
    }
    Collection<UnloadedModuleDescription> descriptions = ModuleManager.getInstance(myProject).getUnloadedModuleDescriptions();
    for (UnloadedModuleDescription description : descriptions) {
      for (VirtualFilePointer pointer : description.getContentRoots()) {
        VirtualFile root = pointer.getFile();
        if (root != null) {
          VirtualFile parent = root.getParent();
          if (!isFileUnderContentRoot(myIndex, parent)) {
            topLevelContentRoots.add(root);
          }
        }
      }
    }
    return topLevelContentRoots;
  }


  private static boolean isFileUnderContentRoot(@NotNull DirectoryIndex index, @Nullable VirtualFile file) {
    return file != null && index.getInfoForFile(file).getContentRoot() != null;
  }

  private PsiElement @NotNull [] directoryChildrenInProject(PsiDirectory psiDirectory, final ViewSettings settings) {
    final VirtualFile dir = psiDirectory.getVirtualFile();
    if (shouldBeShown(dir, settings)) {
      final List<PsiElement> children = new ArrayList<>();
      psiDirectory.processChildren(new PsiElementProcessor<PsiFileSystemItem>() {
        @Override
        public boolean execute(@NotNull PsiFileSystemItem element) {
          if (shouldBeShown(element.getVirtualFile(), settings)) {
            children.add(element);
          }
          return true;
        }
      });
      return PsiUtilCore.toPsiElementArray(children);
    }

    PsiManager manager = psiDirectory.getManager();
    Set<PsiElement> directoriesOnTheWayToContentRoots = new THashSet<>();
    for (VirtualFile root : getTopLevelRoots()) {
      VirtualFile current = root;
      while (current != null) {
        VirtualFile parent = current.getParent();

        if (Comparing.equal(parent, dir)) {
          final PsiDirectory psi = manager.findDirectory(current);
          if (psi != null) {
            directoriesOnTheWayToContentRoots.add(psi);
          }
        }
        current = parent;
      }
    }

    return PsiUtilCore.toPsiElementArray(directoriesOnTheWayToContentRoots);
  }

  private boolean shouldBeShown(@NotNull VirtualFile dir, ViewSettings settings) {
    if (!dir.isValid()) return false;
    DirectoryInfo directoryInfo = myIndex.getInfoForFile(dir);
    return directoryInfo.isInProject(dir)
           ? shouldShowExcludedFiles(settings) || !isProjectOrWorkspaceFile(dir)
           : shouldShowExcludedFiles(settings) && directoryInfo.isExcluded(dir);
  }

  private static boolean shouldShowExcludedFiles(ViewSettings settings) {
    return !Registry.is("ide.hide.excluded.files") && settings instanceof ProjectViewSettings && ((ProjectViewSettings)settings).isShowExcludedFiles();
  }

  // used only for non-flatten packages mode
  private void processPsiDirectoryChildren(PsiElement[] children,
                                           List<? super AbstractTreeNod2<?>> container,
                                           ProjectFileIndex projectFileIndex,
                                           @Nullable ModuleFileIndex moduleFileIndex,
                                           ViewSettings viewSettings,
                                           boolean withSubDirectories,
                                           @Nullable PsiFileSystemItemFilter filter) {
    for (PsiElement child : children) {
      LOG.assertTrue(child.isValid());

      if (!(child instanceof PsiFileSystemItem)) {
        LOG.error("Either PsiFile or PsiDirectory expected as a child of " + child.getParent() + ", but was " + child);
        continue;
      }
      final VirtualFile vFile = ((PsiFileSystemItem) child).getVirtualFile();
      if (vFile == null) {
        continue;
      }
      if (moduleFileIndex != null && !moduleFileIndex.isInContent(vFile)) {
        continue;
      }
      if (filter != null && !filter.shouldShow((PsiFileSystemItem)child)) {
        continue;
      }
      if (child instanceof PsiFile) {
        container.add(new PsiFileNode2((PsiFile) child, viewSettings));
      }
      else if (child instanceof PsiDirectory) {
        if (withSubDirectories) {
          PsiDirectory dir = (PsiDirectory)child;
          if (!vFile.equals(projectFileIndex.getSourceRootForFile(vFile))) { // if is not a source root
            if (viewSettings.isHideEmptyMiddlePackages() && !skipDirectory() && isEmptyMiddleDirectory()) {
              processPsiDirectoryChildren(
                directoryChildrenInProject(dir, viewSettings), container, projectFileIndex, moduleFileIndex, viewSettings, true, filter
              ); // expand it recursively
              continue;
            }
          }
          container.add(new PsiDirectoryNode2(child.getProject(), (PsiDirectory)child, viewSettings, filter));
        }
      }
    }
  }

  // used only in flatten packages mode
  private void addAllSubpackages(List<? super AbstractTreeNod2<?>> container,
                                 PsiDirectory dir,
                                 @Nullable ModuleFileIndex moduleFileIndex,
                                 ViewSettings viewSettings,
                                 @Nullable PsiFileSystemItemFilter filter) {
    final Project project = dir.getProject();
    PsiDirectory[] subdirs = dir.getSubdirectories();
    for (PsiDirectory subdir : subdirs) {
      if (skipDirectory() || filter != null && !filter.shouldShow(subdir)) {
        continue;
      }
      if (moduleFileIndex != null && !moduleFileIndex.isInContent(subdir.getVirtualFile())) {
        container.add(new PsiDirectoryNode2(project, subdir, viewSettings, filter));
        continue;
      }
      if (viewSettings.isHideEmptyMiddlePackages()) {
        if (!isEmptyMiddleDirectory()) {

          container.add(new PsiDirectoryNode2(project, subdir, viewSettings, filter));
        }
      }
      else {
        container.add(new PsiDirectoryNode2(project, subdir, viewSettings, filter));
      }
      addAllSubpackages(container, subdir, moduleFileIndex, viewSettings, filter);
    }
  }

  @NotNull
  public Collection<AbstractTreeNod2<?>> createFileAndDirectoryNodes(@NotNull List<? extends VirtualFile> files, ViewSettings viewSettings) {
    final List<AbstractTreeNod2<?>> children = new ArrayList<>(files.size());
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    for (final VirtualFile virtualFile : files) {
      ContainerUtil.addIfNotNull(children, doCreateNode(virtualFile, psiManager, viewSettings));
    }
    return children;
  }

  @Nullable
  protected AbstractTreeNod2<?> doCreateNode(@NotNull VirtualFile virtualFile,
                                             @NotNull PsiManager psiManager,
                                             @Nullable ViewSettings viewSettings) {
    if (virtualFile.isDirectory()) {
      PsiDirectory directory = psiManager.findDirectory(virtualFile);
      if (directory != null) {
        return new PsiDirectoryNode2(myProject, directory, viewSettings, null);
      }
    }
    else {
      PsiFile file = psiManager.findFile(virtualFile);
      if (file != null) {
        return new PsiFileNode2(file, viewSettings);
      }
    }
    return null;
  }
}
