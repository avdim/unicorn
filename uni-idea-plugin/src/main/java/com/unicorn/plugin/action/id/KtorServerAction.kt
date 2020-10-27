package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.server.startKtorServer


class KtorServerAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    startKtorServer()
  }

}
