package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.unicorn.Uni
import com.unicorn.plugin.action.cmd.openDialogFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UniIntegrationTestAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    Uni.scope.launch {
      val dialog = openDialogFileManager()
      delay(4000)
      dialog.close(0)
    }
  }
}
