package com.unicorn.plugin.action.cmd

import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.action.chooseCommand
import com.unicorn.plugin.action.cmd.misc.*
import com.unicorn.plugin.action.cmd.misc.DialogUiShowCase

class Misc(val context: UniversalContext) : Command {
  override fun execute() {
    chooseCommand(
      context.event,
      MyActionPopupMenu(context.event),
      KtorServer(),
      RestartIDE(),
      DialogUiShowCase()
    )
  }

}
