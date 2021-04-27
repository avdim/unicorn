package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.NavigatableWithText
import com.intellij.util.AstLoadingFilter
import com.intellij.util.IconUtil
import com.unicorn.Uni

abstract class BasePsiNode2(val virtualFile:VirtualFile) : AbstractTreeNod2<VirtualFile>(virtualFile) {
  override fun getFileStatus(): FileStatus =
    if (virtualFile.fileSystem is NonPhysicalFileSystem) {
      FileStatus.SUPPRESSED // do not leak light files via cache
    } else {
      FileStatus.NOT_CHANGED //  also look at FileStatusProvider and VcsFileStatusProvider
    }

  protected abstract fun updateImpl(data: PresentationData)

  public override fun update(presentation: PresentationData) {
    AstLoadingFilter.disallowTreeLoading<RuntimeException> {
      ApplicationManager.getApplication().runReadAction {
        presentation.presentableText = virtualFile.name
        presentation.setIcon(IconUtil.getIcon(virtualFile, 0, Uni.todoDefaultProject))
        if (false) {
          presentation.setIcon(patchIcon(presentation.getIcon(true), virtualFile))
        }
        presentation.locationString = "hint"
        updateImpl(presentation)
      }
    }
  }

  override fun getName(): String? = value.name

}
