package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.Uni
import com.unicorn.plugin.getGithubToken
import com.unicorn.plugin.ui.showPanelDialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Suppress("ComponentNotRegistered")
class GetGithubTokenAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    getGithubToken(
      callback = { token ->
        Uni.log.info { "got GitHub token: $token" }
        MainScope().launch {
          showPanelDialog {
            row {
              textField(getter = { token }, setter = {})
            }
          }
        }
      }
    )
  }

}
