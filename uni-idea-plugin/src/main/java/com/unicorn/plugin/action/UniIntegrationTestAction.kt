package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.plugin.ActionSubscription
import com.unicorn.plugin.action.cmd.openDialogFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UniIntegrationTestAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(e: AnActionEvent) {
//    Uni.scope.launch {
////      val dialog = openDialogFileManager()
////      delay(10_000)
////      dialog.close(0)
//
//      delay(2_000)
//      ActionSubscription.stopSubscription() //todo сделать Action UnloadUniPlugin
//      Uni.job.cancel()
//    }
  }

}
