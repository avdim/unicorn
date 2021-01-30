// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.CompoundIconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.*;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.FontUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.SmartHashSet;
import com.unicorn.Uni;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;

import javax.swing.*;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class PsiDirectoryNode2 extends BasePsiNode2<PsiDirectory> implements NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private final Set<VirtualFile> chain = new SmartHashSet<>();

  private final PsiFileSystemItemFilter myFilter;

  public PsiDirectoryNode2(@NotNull PsiDirectory value, ViewSettings viewSettings, @Nullable PsiFileSystemItemFilter filter) {
    super(value, viewSettings);
    myFilter = filter;
  }

  @Nullable
  public PsiFileSystemItemFilter getFilter() {
    return myFilter;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    PsiDirectory psiDirectory = getValue();
    assert psiDirectory != null : this;
    VirtualFile directoryFile = psiDirectory.getVirtualFile();
    Object parentValue = getParentValue();
    synchronized (chain) {
      if (chain.isEmpty()) {
        VirtualFile ancestor = getVirtualFile(parentValue);
        if (ancestor != null) {
          for (VirtualFile file = directoryFile; file != null && VfsUtilCore.isAncestor(ancestor, file, true); file = file.getParent()) {
            chain.add(file);
          }
        }
        if (chain.isEmpty()) chain.add(directoryFile);
      }
    }

/*
    if (ProjectRootsUtil.isModuleContentRoot(directoryFile, project2)) {
      ProjectFileIndex fi = ProjectRootManager.getInstance(project2).getFileIndex();
      Module module = fi.getModuleForFile(directoryFile);

      data.setPresentableText(directoryFile.getName());
      if (module != null) {
        if (!(parentValue instanceof Module)) {
          if (ModuleType.isInternal(module) || !shouldShowModuleName()) {
            data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
          else {
            data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);// or REGULAR_BOLD_ATTRIBUTES
          }
        }
        else {
          data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
        boolean shouldShowUrl = getSettings().isShowURL() && (parentValue instanceof Module || parentValue instanceof Project);
        data.setLocationString(ProjectViewDirectoryHelper.getInstance(project2).getLocationString(psiDirectory, shouldShowUrl, shouldShowSourcesRoot()));
        setupIcon(data, psiDirectory);
        return;
      }
    }
*/

    String name = parentValue instanceof Project
      ? psiDirectory.getVirtualFile().getPresentableUrl()
      : ProjectViewDirectoryHelper.getInstance(psiDirectory.getProject()).getNodeName(getSettings(), parentValue, psiDirectory);
    if (name == null) {
      setValue(null);
      return;
    }

    if(Uni.INSTANCE.getBOLD_DIRS()) {
      data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
    data.setPresentableText(name);
    data.setLocationString(getLocationString2(psiDirectory, false, false));

    setupIcon(data, psiDirectory);
  }

  @Nullable
  public String getLocationString2(@NotNull PsiDirectory psiDirectory, boolean includeUrl, boolean includeRootType) {
    StringBuilder result = new StringBuilder();

    final VirtualFile directory = psiDirectory.getVirtualFile();

    if (ProjectRootsUtil.isLibraryRoot(directory, psiDirectory.getProject())) {
      result.append(ProjectBundle.message("module.paths.root.node", "library").toLowerCase(Locale.getDefault()));
    } else if (includeRootType) {
      SourceFolder sourceRoot = ProjectRootsUtil.getModuleSourceRoot(psiDirectory.getVirtualFile(), psiDirectory.getProject());
      if (sourceRoot != null) {
        ModuleSourceRootEditHandler<?> handler = ModuleSourceRootEditHandler.getEditHandler(sourceRoot.getRootType());
        if (handler != null) {
          JavaSourceRootProperties properties = sourceRoot.getJpsElement().getProperties(JavaModuleSourceRootTypes.SOURCES);
          if (properties != null && properties.isForGeneratedSources()) {
            result.append("generated ");
          }
          result.append(handler.getFullRootTypeName().toLowerCase(Locale.getDefault()));
        }
      }
    }

    if (includeUrl) {
      if (result.length() > 0) result.append(",").append(FontUtil.spaceAndThinSpace());
      result.append(FileUtil.getLocationRelativeToUserHome(directory.getPresentableUrl()));
    }

    return result.length() == 0 ? null : result.toString();
  }

  protected void setupIcon(PresentationData data, PsiDirectory psiDirectory) {
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    if (PlatformUtils.isAppCode()) {
//      final Icon icon = IconUtil.getIcon(virtualFile, 0, project2);
      final Icon icon2 = IconUtil.getIcon(virtualFile, 0, Uni.getTodoDefaultProject());
      data.setIcon(icon2);
    } else {
      Icon icon = CompoundIconProvider.findIcon(psiDirectory, 0);
      if (icon != null) data.setIcon(icon);
    }
  }

  @Override
  public Collection<AbstractTreeNod2<?>> getChildrenImpl() {
    return getDirectoryChildren(getValue(), getSettings(), true, getFilter());
  }

  @NotNull
  public Collection<AbstractTreeNod2<?>> getDirectoryChildren(PsiDirectory psiDirectory,
                                                              ViewSettings settings,
                                                              boolean withSubDirectories,
                                                              @Nullable PsiFileSystemItemFilter filter) {
    return AbstractTreeUi.calculateYieldingToWriteAction(() -> doGetDirectoryChildren(psiDirectory, settings, withSubDirectories, filter));
  }

  public boolean skipDirectory() {
    return true;
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

          children.add(new PsiDirectoryNode2(subdir, settings, filter));
        }
      }
      processPsiDirectoryChildren(psiDirectory.getFiles(), children, fileIndex, moduleFileIndex, settings,
        withSubDirectories, filter);
    }
    return children;
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
        container.add(new PsiDirectoryNode2(subdir, viewSettings, filter));
        continue;
      }
      if (viewSettings.isHideEmptyMiddlePackages()) {
        if (!isEmptyMiddleDirectory()) {

          container.add(new PsiDirectoryNode2(subdir, viewSettings, filter));
        }
      }
      else {
        container.add(new PsiDirectoryNode2(subdir, viewSettings, filter));
      }
      addAllSubpackages(container, subdir, moduleFileIndex, viewSettings, filter);
    }
  }

  public boolean isEmptyMiddleDirectory() {
    return false;
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
      Uni.getLog().assertTrue(child.isValid());

      if (!(child instanceof PsiFileSystemItem)) {
        Uni.getLog().error("Either PsiFile or PsiDirectory expected as a child of " + child.getParent() + ", but was " + child);
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
          container.add(new PsiDirectoryNode2((PsiDirectory)child, viewSettings, filter));
        }
      }
    }
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

  private static boolean isFileUnderContentRoot(@NotNull DirectoryIndex index, @Nullable VirtualFile file) {
    return file != null && index.getInfoForFile(file).getContentRoot() != null;
  }

  @NotNull
  public List<VirtualFile> getTopLevelRoots() {
    return new ArrayList<>();
//    List<VirtualFile> topLevelContentRoots = new ArrayList<>();
//    ProjectRootManager prm = ProjectRootManager.getInstance(project2);
//
//    for (VirtualFile root : prm.getContentRoots()) {
//      VirtualFile parent = root.getParent();
//      if (!isFileUnderContentRoot(myIndex, parent)) {
//        topLevelContentRoots.add(root);
//      }
//    }
//    Collection<UnloadedModuleDescription> descriptions = ModuleManager.getInstance(project2).getUnloadedModuleDescriptions();
//    for (UnloadedModuleDescription description : descriptions) {
//      for (VirtualFilePointer pointer : description.getContentRoots()) {
//        VirtualFile root = pointer.getFile();
//        if (root != null) {
//          VirtualFile parent = root.getParent();
//          if (!isFileUnderContentRoot(myIndex, parent)) {
//            topLevelContentRoots.add(root);
//          }
//        }
//      }
//    }
//    return topLevelContentRoots;
  }

  private boolean shouldBeShown(@NotNull VirtualFile dir, ViewSettings settings) {
    if (!dir.isValid()) return false;
    Uni.getLog().warning("shouldBeShown in empty");
    return true;
//    DirectoryInfo directoryInfo = myIndex.getInfoForFile(dir);
//    boolean cond = directoryInfo.isInProject(dir);
//    boolean shouldShowExcludedFiles = shouldShowExcludedFiles(settings);
//    boolean a = shouldShowExcludedFiles || !isProjectOrWorkspaceFile(dir);
//    boolean b = shouldShowExcludedFiles && directoryInfo.isExcluded(dir);
//    boolean result = cond ? a : b;
//    return result;
  }

  private static boolean shouldShowExcludedFiles(ViewSettings settings) {
    return !Registry.is("ide.hide.excluded.files") && settings instanceof ProjectViewSettings && ((ProjectViewSettings)settings).isShowExcludedFiles();
  }

  public boolean isFQNameShown() {
    return false;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final PsiDirectory value = getValue();
    if (value == null) {
      return false;
    }

    VirtualFile directory = value.getVirtualFile();
    if (directory.getFileSystem() instanceof LocalFileSystem) {
      file = VfsUtil.getLocalFile(file);
    }

    if (!VfsUtilCore.isAncestor(directory, file, false)) {
      return false;
    }

    final @Nullable Project project = value.getProject();
    PsiFileSystemItemFilter filter = getFilter();
    if (filter != null) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile != null && !filter.shouldShow(psiFile)) return false;

      PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(file);
      if (psiDirectory != null && !filter.shouldShow(psiDirectory)) return false;
    }

    if (Registry.is("ide.hide.excluded.files")) {
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      return !fileIndex.isExcluded(file);
    } else {
      return !FileTypeRegistry.getInstance().isFileIgnored(file);
    }
  }

  /**
   * @return a virtual file that identifies the given element
   */
  @Nullable
  private static VirtualFile getVirtualFile(Object element) {
    if (element instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory) element;
      return directory.getVirtualFile();
    }
    return element instanceof VirtualFile ? (VirtualFile) element : null;
  }

  @Override
  public boolean canRepresent(final Object element) {
    //Сюда код не доходил при отладке
    VirtualFile file = getVirtualFile(element);
    if (file != null) {
      synchronized (chain) {
        if (chain.contains(file)) return true;
      }
    }
    if (super.canRepresent(element)) return true;
    if (element instanceof VirtualFile && getParentValue() instanceof PsiDirectory) {
      Uni.getLog().error("canRepresent return true");
      return true;
//      return ProjectViewDirectoryHelper.getInstance(project2)
//        .canRepresent((VirtualFile) element, getValue(), (PsiDirectory) getParentValue(), getSettings());
    } else {
      Uni.getLog().error("canRepresent return false. element:" + element + ", getValue(): " + getValue() + ", getParentValue(): " + getParentValue());
      return false;
    }
  }

  @Override
  public boolean isValid() {
    return true;
//    if (!super.isValid()) return false;
//    return ProjectViewDirectoryHelper.getInstance(getProject())
//      .isValidDirectory(getValue(), getParentValue(), getSettings(), getFilter());
  }

  @Override
  public boolean canNavigate() {
    VirtualFile file = getVirtualFile();
    return file != null;
//    ProjectSettingsService service = ProjectSettingsService.getInstance(project2);
//    boolean result = file != null && (ProjectRootsUtil.isModuleContentRoot(file, project2) && service.canOpenModuleSettings() ||
//      ProjectRootsUtil.isModuleSourceRoot(file, project2) && service.canOpenContentEntriesSettings() ||
//      ProjectRootsUtil.isLibraryRoot(file, project2) && service.canOpenModuleLibrarySettings());
//    return result;//false
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Uni.getLog().warning("empty navigate");
    Module module = ModuleUtilCore.findModuleForPsiElement(getValue());
    if (module != null) {
      final VirtualFile file = getVirtualFile();
//      ProjectSettingsService service = ProjectSettingsService.getInstance(project2);
//      if (ProjectRootsUtil.isModuleContentRoot(file, project2)) {
//        service.openModuleSettings(module);
//      }
//      else if (ProjectRootsUtil.isLibraryRoot(file, project2)) {
//        final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(file, module.getProject());
//        if (orderEntry != null) {
//          service.openLibraryOrSdkSettings(orderEntry);
//        }
//      }
//      else {
//        service.openContentEntriesSettings(module);
//      }
    }
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    VirtualFile file = getVirtualFile();
//    if (file != null) {
//      if (ProjectRootsUtil.isModuleContentRoot(file, project2) || ProjectRootsUtil.isModuleSourceRoot(file, project2)) {
//        return ActionsBundle.message("action.ModuleSettings.navigate");
//      }
//      if (ProjectRootsUtil.isLibraryRoot(file, project2)) {
//        return ActionsBundle.message("action.LibrarySettings.navigate");
//      }
//    }

    return "Todo GetNavigateActionText";
  }

  @Override
  public int getWeight() {
    ViewSettings settings = getSettings();
    if (settings == null || settings.isFoldersAlwaysOnTop()) {
      return 20;
    }
    return isFQNameShown() ? 70 : 0;
  }

  @Override
  public String getTitle() {
    final PsiDirectory directory = getValue();
    if (directory != null) {
      return getQualifiedName2(directory, true);
    }
    return super.getTitle();
  }

  @NotNull
  private String getQualifiedName2(@NotNull final PsiDirectory directory, final boolean presentable) {
    if (presentable) {
      return FileUtil.getLocationRelativeToUserHome(directory.getVirtualFile().getPresentableUrl());
    }
    return "";
  }

  @Override
  public boolean isAlwaysShowPlus() {
    final VirtualFile file = getVirtualFile();
    return file == null || file.getChildren().length > 0;
  }
}
