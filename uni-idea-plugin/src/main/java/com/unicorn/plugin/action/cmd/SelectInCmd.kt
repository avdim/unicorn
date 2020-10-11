package com.unicorn.plugin.action.cmd

import com.unicorn.plugin.performActionById

class SelectInCmd : Command {
  override fun execute() {
    performActionById("SelectIn")
  }

}
