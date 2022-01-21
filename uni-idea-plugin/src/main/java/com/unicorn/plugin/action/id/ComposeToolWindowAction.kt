package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.getToolWindow
import com.intellij.my.file.ConfUniFiles
import com.intellij.openapi.wm.ToolWindowId

class ComposeToolWindowAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    ToolWindowId.PROJECT_VIEW
    event.project?.getToolWindow(ConfUniFiles.COMPOSE_WINDOW_ID)?.let {
      if (it.isVisible) {
        it.hide()
      } else {
        it.show()
      }
    }
  }

}
