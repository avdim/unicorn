package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.unicorn.plugin.action.cmd.Command
import com.unicorn.plugin.ui.choosePopup

fun chooseCommand(event: AnActionEvent, vararg commands: Command) {
  choosePopup(event, "Commands", commands.toList(),
    getName = { it.name() }) {
    it.execute()
  }
}
