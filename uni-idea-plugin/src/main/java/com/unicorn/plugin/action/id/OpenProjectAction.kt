package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.unicorn.plugin.suggestString


class OpenProjectAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val file: VirtualFile? = com.unicorn.Uni.selectedFile
    suggestOpenProject(file?.path)
  }

}

fun suggestOpenProject(suggestPath:String?) {
  val path: String? = suggestString("open project?", suggestPath ?: "missing directory!!!")
  if (path != null) {
    com.intellij.ide.impl.ProjectUtil.openOrImport(path, ProjectManager.getInstance().defaultProject, true)
  }
}
