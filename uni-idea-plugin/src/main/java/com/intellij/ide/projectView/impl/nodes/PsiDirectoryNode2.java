// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.CompoundIconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;

import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouperKt;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.SmartHashSet;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Set;

public class PsiDirectoryNode2 extends BasePsiNode2<PsiDirectory> implements NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private final Set<VirtualFile> chain = new SmartHashSet<>();

  private final PsiFileSystemItemFilter myFilter;
  private final @NotNull Project project2;

  public PsiDirectoryNode2(@NotNull Project project, @NotNull PsiDirectory value, ViewSettings viewSettings) {
    this(project, value, viewSettings, null);
  }

  public PsiDirectoryNode2(@NotNull Project project, @NotNull PsiDirectory value, ViewSettings viewSettings, @Nullable PsiFileSystemItemFilter filter) {
    super(project, value, viewSettings);
    project2 = project;
    myFilter = filter;
  }

  @Nullable
  public PsiFileSystemItemFilter getFilter() {
    return myFilter;
  }

  protected boolean shouldShowModuleName() {
    return !PlatformUtils.isCidr();
  }

  protected boolean shouldShowSourcesRoot() {
    return true;
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
        data.setLocationString(ProjectViewDirectoryHelper.getInstance(project2).getLocationString(psiDirectory,
                                                                                                 shouldShowUrl,
                                                                                                 shouldShowSourcesRoot()));
        setupIcon(data, psiDirectory);

        return;
      }
    }

    String name = parentValue instanceof Project
                  ? psiDirectory.getVirtualFile().getPresentableUrl()
                  : ProjectViewDirectoryHelper.getInstance(psiDirectory.getProject()).getNodeName(getSettings(), parentValue, psiDirectory);
    if (name == null) {
      setValue(null);
      return;
    }

    data.setPresentableText(name);
    data.setLocationString(ProjectViewDirectoryHelper.getInstance(project2).getLocationString(psiDirectory, false, false));

    setupIcon(data, psiDirectory);
  }

  protected static boolean canRealModuleNameBeHidden() {
    return Registry.is("ide.hide.real.module.name");
  }

  protected void setupIcon(PresentationData data, PsiDirectory psiDirectory) {
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    if (PlatformUtils.isAppCode()) {
      final Icon icon = IconUtil.getIcon(virtualFile, 0, project2);
      data.setIcon(icon);
    }
    else {
      Icon icon = CompoundIconProvider.findIcon(psiDirectory, 0);
      if (icon != null) data.setIcon(icon);
    }
  }

  @Override
  public Collection<AbstractTreeNod2<?>> getChildrenImpl() {
    return ProjectViewDirectoryHelper2.getInstance(project2).getDirectoryChildren(getValue(), getSettings(), true, getFilter());
  }

  public boolean isFQNameShown() {
    return ProjectViewDirectoryHelper.getInstance(project2).isShowFQName(getSettings(), getParentValue(), getValue());
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
    }
    else {
      return !FileTypeRegistry.getInstance().isFileIgnored(file);
    }
  }

  /**
   * @return a virtual file that identifies the given element
   */
  @Nullable
  private static VirtualFile getVirtualFile(Object element) {
    if (element instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)element;
      return directory.getVirtualFile();
    }
    return element instanceof VirtualFile ? (VirtualFile)element : null;
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
      return ProjectViewDirectoryHelper.getInstance(project2)
        .canRepresent((VirtualFile) element, getValue(), (PsiDirectory) getParentValue(), getSettings());
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
    ProjectSettingsService service = ProjectSettingsService.getInstance(project2);
    return file != null && (ProjectRootsUtil.isModuleContentRoot(file, project2) && service.canOpenModuleSettings() ||
                            ProjectRootsUtil.isModuleSourceRoot(file, project2)  && service.canOpenContentEntriesSettings() ||
                            ProjectRootsUtil.isLibraryRoot(file, project2) && service.canOpenModuleLibrarySettings());
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getValue());
    if (module != null) {
      final VirtualFile file = getVirtualFile();
      ProjectSettingsService service = ProjectSettingsService.getInstance(project2);
      if (ProjectRootsUtil.isModuleContentRoot(file, project2)) {
        service.openModuleSettings(module);
      }
      else if (ProjectRootsUtil.isLibraryRoot(file, project2)) {
        final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(file, module.getProject());
        if (orderEntry != null) {
          service.openLibraryOrSdkSettings(orderEntry);
        }
      }
      else {
        service.openContentEntriesSettings(module);
      }
    }
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    VirtualFile file = getVirtualFile();
    if (file != null) {
      if (ProjectRootsUtil.isModuleContentRoot(file, project2) || ProjectRootsUtil.isModuleSourceRoot(file, project2)) {
        return ActionsBundle.message("action.ModuleSettings.navigate");
      }
      if (ProjectRootsUtil.isLibraryRoot(file, project2)) {
        return ActionsBundle.message("action.LibrarySettings.navigate");
      }
    }

    return null;
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
      return PsiDirectoryFactory.getInstance(project2).getQualifiedName(directory, true);
    }
    return super.getTitle();
  }

  @Override
  public boolean isAlwaysShowPlus() {
    final VirtualFile file = getVirtualFile();
    return file == null || file.getChildren().length > 0;
  }
}
