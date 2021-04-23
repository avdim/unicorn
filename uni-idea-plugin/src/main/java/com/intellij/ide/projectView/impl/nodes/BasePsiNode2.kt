package com.intellij.ide.projectView.impl.nodes

import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore

abstract class BasePsiNode2<T : PsiElement> protected constructor(value: T) : AbstractPsiBasedNode2<T>(value) {

  private val myVirtualFile: VirtualFile? = PsiUtilCore.getVirtualFile(value)

  override fun getFileStatus(): FileStatus =
    if (myVirtualFile != null && myVirtualFile.fileSystem is NonPhysicalFileSystem) {
      FileStatus.SUPPRESSED // do not leak light files via cache
    } else {
      FileStatus.NOT_CHANGED //  also look at FileStatusProvider and VcsFileStatusProvider
    }

  public override fun getVirtualFile(): VirtualFile? {//todo not null
    return myVirtualFile
  }

  override fun extractPsiFromValue(): PsiElement? {
    return value
  }

}
