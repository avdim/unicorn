// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.CompoundIconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.FontUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.SmartHashSet;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;

import javax.swing.*;
import java.util.Locale;
import java.util.Set;

public abstract class PsiDirectoryNode2 extends BasePsiNode2<PsiDirectory> implements NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private final Set<VirtualFile> chain = new SmartHashSet<>();

  private final PsiFileSystemItemFilter myFilter;

  public PsiDirectoryNode2(@NotNull PsiDirectory value, @Nullable PsiFileSystemItemFilter filter) {
    super(value);
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

    boolean isViewRoot = parentValue instanceof Project;
    String name = isViewRoot ? psiDirectory.getVirtualFile().getPresentableUrl() : psiDirectory.getName();

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

  public boolean isFQNameShown() {
    return false;
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
    if (Uni.fileManagerConf2.isFoldersAlwaysOnTop) {
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
