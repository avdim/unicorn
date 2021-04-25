package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.unicorn.plugin.suggestString


class OpenProjectAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val file = com.unicorn.Uni.selectedFile
    val path: String? = suggestString("open project?", file?.path ?: "missing directory!!!")
    if (path != null) {
      com.intellij.ide.impl.ProjectUtil.openOrImport(path, ProjectManager.getInstance().defaultProject, false)
    }
  }

}
