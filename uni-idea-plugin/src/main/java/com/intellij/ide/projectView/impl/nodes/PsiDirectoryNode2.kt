// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.CompoundIconProvider
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.NavigatableWithText
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.FontUtil
import com.intellij.util.IconUtil
import com.intellij.util.PlatformUtils
import com.intellij.util.containers.SmartHashSet
import com.unicorn.Uni
import com.unicorn.Uni.BOLD_DIRS
import com.unicorn.Uni.log
import com.unicorn.Uni.todoDefaultProject
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*

abstract class PsiDirectoryNode2(value: PsiDirectory) : BasePsiNode2<PsiDirectory>(value), NavigatableWithText {
    // the chain from a parent directory to this one usually contains only one virtual file
    private val chain: MutableSet<VirtualFile> = SmartHashSet()
    override fun updateImpl(data: PresentationData) {
        val psiDirectory = value ?: kotlin.error(this)
        val directoryFile = psiDirectory.virtualFile
        val parentValue = parentValue
        synchronized(chain) {
            if (chain.isEmpty()) {
                val ancestor = getVirtualFile(parentValue)
                if (ancestor != null) {
                    var file: VirtualFile? = directoryFile
                    while (file != null && VfsUtilCore.isAncestor(ancestor, file, true)) {
                        chain.add(file)
                        file = file.parent
                    }
                }
                if (chain.isEmpty()) chain.add(directoryFile)
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
        val isViewRoot = parentValue is Project
        val name = if (isViewRoot) psiDirectory.virtualFile.presentableUrl else psiDirectory.name
        if (BOLD_DIRS) {
            data.addText(directoryFile.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
        data.presentableText = name
        data.locationString = getLocationString2(psiDirectory, false, false)
        setupIcon(data, psiDirectory)
    }

    fun getLocationString2(psiDirectory: PsiDirectory, includeUrl: Boolean, includeRootType: Boolean): String? {
        val result = StringBuilder()
        val directory = psiDirectory.virtualFile
        if (ProjectRootsUtil.isLibraryRoot(directory, psiDirectory.project)) {
            result.append(ProjectBundle.message("module.paths.root.node", "library").toLowerCase(Locale.getDefault()))
        } else if (includeRootType) {
            val sourceRoot = ProjectRootsUtil.getModuleSourceRoot(psiDirectory.virtualFile, psiDirectory.project)
            if (sourceRoot != null) {
                val handler = ModuleSourceRootEditHandler.getEditHandler(sourceRoot.rootType)
                if (handler != null) {
                    val properties = sourceRoot.jpsElement.getProperties(JavaModuleSourceRootTypes.SOURCES)
                    if (properties != null && properties.isForGeneratedSources) {
                        result.append("generated ")
                    }
                    result.append(handler.fullRootTypeName.toLowerCase(Locale.getDefault()))
                }
            }
        }
        if (includeUrl) {
            if (result.length > 0) result.append(",").append(FontUtil.spaceAndThinSpace())
            result.append(FileUtil.getLocationRelativeToUserHome(directory.presentableUrl))
        }
        return if (result.length == 0) null else result.toString()
    }

    protected fun setupIcon(data: PresentationData, psiDirectory: PsiDirectory?) {
        val virtualFile = psiDirectory!!.virtualFile
        if (PlatformUtils.isAppCode()) {
//      final Icon icon = IconUtil.getIcon(virtualFile, 0, project2);
            val icon2 = IconUtil.getIcon(virtualFile, 0, todoDefaultProject)
            data.setIcon(icon2)
        } else {
            val icon = CompoundIconProvider.findIcon(psiDirectory, 0)
            if (icon != null) data.setIcon(icon)
        }
    }

    val isFQNameShown: Boolean
        get() = false

    override fun canRepresent(element: Any): Boolean {
        //Сюда код не доходил при отладке
        val file = getVirtualFile(element)
        if (file != null) {
            synchronized(chain) { if (chain.contains(file)) return true }
        }
        if (super.canRepresent(element)) return true
        return if (element is VirtualFile && parentValue is PsiDirectory) {
            log.error("canRepresent return true")
            true
            //      return ProjectViewDirectoryHelper.getInstance(project2)
//        .canRepresent((VirtualFile) element, getValue(), (PsiDirectory) getParentValue(), getSettings());
        } else {
            log.error("canRepresent return false. element:$element, getValue(): $value, getParentValue(): $parentValue")
            false
        }
    }

    override fun isValid(): Boolean {
        return true
        //    if (!super.isValid()) return false;
//    return ProjectViewDirectoryHelper.getInstance(getProject())
//      .isValidDirectory(getValue(), getParentValue(), getSettings(), getFilter());
    }

    override fun canNavigate(): Boolean {
        val file = virtualFile
        return file != null
        //    ProjectSettingsService service = ProjectSettingsService.getInstance(project2);
//    boolean result = file != null && (ProjectRootsUtil.isModuleContentRoot(file, project2) && service.canOpenModuleSettings() ||
//      ProjectRootsUtil.isModuleSourceRoot(file, project2) && service.canOpenContentEntriesSettings() ||
//      ProjectRootsUtil.isLibraryRoot(file, project2) && service.canOpenModuleLibrarySettings());
//    return result;//false
    }

    override fun canNavigateToSource(): Boolean {
        return false
    }

    override fun navigate(requestFocus: Boolean) {
        log.warning("empty navigate")
        val module = ModuleUtilCore.findModuleForPsiElement(value!!)
        if (module != null) {
            val file = virtualFile
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

    override fun getNavigateActionText(focusEditor: Boolean): String? {
        val file = virtualFile
        //    if (file != null) {
//      if (ProjectRootsUtil.isModuleContentRoot(file, project2) || ProjectRootsUtil.isModuleSourceRoot(file, project2)) {
//        return ActionsBundle.message("action.ModuleSettings.navigate");
//      }
//      if (ProjectRootsUtil.isLibraryRoot(file, project2)) {
//        return ActionsBundle.message("action.LibrarySettings.navigate");
//      }
//    }
        return "Todo GetNavigateActionText"
    }

    override fun getWeight(): Int {
        if (Uni.fileManagerConf2.isFoldersAlwaysOnTop) {
            return 20
        }
        return if (isFQNameShown) 70 else 0
    }

    override fun isAlwaysShowPlus(): Boolean {
        val file = virtualFile
        return file == null || file.children.size > 0
    }

    companion object {
        /**
         * @return a virtual file that identifies the given element
         */
        private fun getVirtualFile(element: Any?): VirtualFile? {
            if (element is PsiDirectory) {
                return element.virtualFile
            }
            return if (element is VirtualFile) element else null
        }
    }
}