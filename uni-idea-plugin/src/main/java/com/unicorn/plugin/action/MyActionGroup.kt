package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class MyActionGroup(val name:String, val actions: List<AnAction>) : ActionGroup(name, true) {

  override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    return actions.toTypedArray()
  }

}
