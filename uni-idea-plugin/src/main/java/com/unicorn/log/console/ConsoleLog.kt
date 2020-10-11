package com.unicorn.log.console

import com.unicorn.log.lib.Log

object ConsoleLog {
  init {
    Log.addLogConsumer {
      println("-log- $it")
    }
  }

  fun start() {

  }
}