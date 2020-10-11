package com.unicorn.plugin.action.cmd

import com.unicorn.plugin.performActionById

class ChooseRuntime : Command {
  override fun execute() {
    performActionById("bootRuntime2.main.ChooseBootRuntimeAction")
  }

}
