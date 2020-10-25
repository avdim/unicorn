package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.awt.Color


class RestartAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val result = Messages.showOkCancelDialog(
      "Restart now?",
      "restart",
      "Yes",
      "No",
      com.intellij.ui.TextIcon("i", Color.BLUE, Color.WHITE, 2)
    )
    if (result == Messages.YES) {
      val app = ApplicationManager.getApplication() as ApplicationEx
      app.restart(false)
    }
  }

}
