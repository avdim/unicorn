package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.ui.render.showWelcomeDialog

@Suppress("ComponentNotRegistered", "unused")
class WelcomeAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    showWelcomeDialog()
  }

}
