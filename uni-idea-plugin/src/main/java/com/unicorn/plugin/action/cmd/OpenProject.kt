package com.unicorn.plugin.action.cmd

import com.intellij.openapi.project.ProjectManager
import com.unicorn.plugin.suggestString

class OpenProject : Command {
  override fun execute() {
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