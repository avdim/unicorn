package com.intellij.ide.projectView.impl.nodes

import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile

abstract class BasePsiNode2(override val virtualFile:VirtualFile) : AbstractPsiBasedNode2<VirtualFile>(virtualFile) {
  override fun getFileStatus(): FileStatus =
    if (virtualFile.fileSystem is NonPhysicalFileSystem) {
      FileStatus.SUPPRESSED // do not leak light files via cache
    } else {
      FileStatus.NOT_CHANGED //  also look at FileStatusProvider and VcsFileStatusProvider
    }


  override fun getName(): String? = value.name

}
