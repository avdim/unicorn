package com.unicorn.plugin.action.cmd.misc

import com.intellij.dvcs.ui.LightActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.unicorn.plugin.action.UniversalAction
import com.unicorn.plugin.action.cmd.Command
import com.unicorn.plugin.ui.showDialog
import com.unicorn.plugin.toolWindowAction
import ru.tutu.idea.file.ConfUniFiles

class MyActionPopupMenu(val event: AnActionEvent) : Command {
  override fun execute() {
    val lightActionGroup = LightActionGroup()
    lightActionGroup.add(toolWindowAction(ConfUniFiles.UNI_WINDOW_ID, event)!!)
    lightActionGroup.add(UniversalAction())
    lightActionGroup.update(event)
    showDialog(
      ActionManager.getInstance().createActionPopupMenu(
        "MyActionPopup",
        lightActionGroup
      ).component
    )
  }

}
