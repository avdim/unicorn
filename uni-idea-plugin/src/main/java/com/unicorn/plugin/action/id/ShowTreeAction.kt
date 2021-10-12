package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.ui.showTreeDialog

@Suppress("ComponentNotRegistered", "unused")
class ShowTreeAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
      showTreeDialog()
  }

}
