package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.perform


class QuickTypeDefinitionAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    ActionManager.getInstance().getAction("QuickTypeDefinition")
      .perform(
        context = event.dataContext,
        event = event
      )
  }

}