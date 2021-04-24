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

abstract class PsiDirectoryNode2(val virtualDir:VirtualFile) : BasePsiNode2(virtualDir), NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private val chain: MutableSet<VirtualFile> = SmartHashSet()
  override fun updateImpl(data: PresentationData) {
    val directoryFile = virtualDir//psiDirectory.virtualFile
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
    if (BOLD_DIRS) {
      data.addText(directoryFile.name + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
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

  override fun canNavigate(): Boolean = false
  override fun canNavigateToSource(): Boolean = false
  override fun navigate(requestFocus: Boolean, preserveState: Boolean) {}
  override fun getNavigateActionText(focusEditor: Boolean): String? = "Nav"

  override fun getWeight(): Int {
    if (Uni.fileManagerConf2.isFoldersAlwaysOnTop) {
      return 20
    }
    return if (isFQNameShown) 70 else 0
  }

  override val isAlwaysShowPlus get(): Boolean = getVirtualFile().children.isNotEmpty()

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
