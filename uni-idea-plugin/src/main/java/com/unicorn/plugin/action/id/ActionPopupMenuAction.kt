package com.unicorn.plugin.action.id

import com.intellij.dvcs.ui.LightActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.toolWindowAction
import com.unicorn.plugin.ui.showDialog
import com.intellij.my.file.ConfUniFiles

class ActionPopupMenuAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val lightActionGroup = LightActionGroup()
    lightActionGroup.add(toolWindowAction(ConfUniFiles.UNI_WINDOW_ID, event)!!)
//    lightActionGroup.add(UniversalAction())
    lightActionGroup.update(event)
    showDialog(
      ActionManager.getInstance().createActionPopupMenu(
        "MyActionPopup",
        lightActionGroup
      ).component
    )
  }

}
