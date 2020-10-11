package com.unicorn.plugin.action.cmd

import com.intellij.openapi.actionSystem.ActionManager
import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.perform

class QuickTypeDefinition(val context: UniversalContext) : Command {
  override fun execute() {
    ActionManager.getInstance().getAction("QuickTypeDefinition")
      .perform(
        context = context.event.dataContext,
        event = context.event
      )
  }

}
