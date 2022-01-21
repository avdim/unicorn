package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.ui.showFileTreeDialog

@Suppress("ComponentNotRegistered", "unused")
class FileTreeAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    showFileTreeDialog()
  }

}
