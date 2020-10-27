package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

abstract class UniAction : AnAction(), DumbAware {

  final override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
    e.presentation.text = this::class.simpleName
  }

}
