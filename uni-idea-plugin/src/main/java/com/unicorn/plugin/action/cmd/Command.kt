package com.unicorn.plugin.action.cmd

interface Command {
  fun execute()
  fun available(): Boolean = true
  fun name(): String = this::class.simpleName ?: toString() //this::class.qualifiedName
}
