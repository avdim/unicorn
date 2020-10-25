package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.getToolWindow
import ru.tutu.idea.file.ConfUniFiles

class FileManagerToolWindowAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

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
