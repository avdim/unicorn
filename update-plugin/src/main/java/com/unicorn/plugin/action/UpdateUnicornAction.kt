package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class UpdateUnicornAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    println("hello UpdateUnicornAction")
  }

}
