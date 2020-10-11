package com.unicorn.plugin.action.cmd

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.ui.Messages
import com.unicorn.Uni
import com.unicorn.plugin.configureIDE
import java.awt.Color


class RestartIDE : Command {
  override fun execute() {
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