package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.performActionById

class SelectInAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    performActionById("SelectIn")
  }

}
