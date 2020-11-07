package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.performActionById

@Suppress("ComponentNotRegistered")
class ReloadGradleAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    performActionById("ExternalSystem.RefreshAllProjects")
  }

}
