package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.getToolWindow
import com.intellij.my.file.ConfUniFiles

class FileManagerToolWindowAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    event.project?.getToolWindow(ConfUniFiles.UNI_WINDOW_ID)?.let {
      if (it.isVisible) {
        it.hide()
      } else {
        it.show()
      }
    }
  }

}
