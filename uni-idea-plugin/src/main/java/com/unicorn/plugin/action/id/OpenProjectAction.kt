package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.unicorn.plugin.suggestString


class OpenProjectAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val file = com.unicorn.Uni.selectedFile
    val path =
      suggestString("open project?", file?.path ?: "missing directory!!!")
    if (path != null) {
      com.intellij.ide.impl.ProjectUtil.openOrImport(
        path,
        ProjectManager.getInstance().defaultProject,
        false
      )
    }
  }

}
