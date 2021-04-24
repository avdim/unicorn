package com.intellij.ide.projectView.impl.nodes

import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile

abstract class BasePsiNode2(virtualFile:VirtualFile) : AbstractPsiBasedNode2<VirtualFile>(virtualFile) {
  private val myVirtualFile: VirtualFile = virtualFile

  override fun getFileStatus(): FileStatus =
    if (myVirtualFile.fileSystem is NonPhysicalFileSystem) {
      FileStatus.SUPPRESSED // do not leak light files via cache
    } else {
      FileStatus.NOT_CHANGED //  also look at FileStatusProvider and VcsFileStatusProvider
    }

  public override fun getVirtualFile(): VirtualFile {
    return myVirtualFile
  }

}
