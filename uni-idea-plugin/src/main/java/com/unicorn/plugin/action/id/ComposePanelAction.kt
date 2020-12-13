package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.compose.helloComposePanel
import com.unicorn.plugin.ui.showDialog

@Suppress("ComponentNotRegistered")
class ComposePanelAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val dialog = showDialog(helloComposePanel())
    dialog.setSize(800, 600)
  }

}
