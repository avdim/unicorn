package com.unicorn.plugin.action.cmd.misc

import com.unicorn.plugin.action.cmd.Command
import com.unicorn.plugin.server.startKtorServer

class KtorServer : Command {
  override fun execute() {
    startKtorServer()
  }
}
