package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.impl.ProjectWindowAction
import com.unicorn.Uni
import com.unicorn.plugin.action.uniContext
import com.unicorn.plugin.ui.choosePopup


class ChooseProjectAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val context = event.uniContext
//    WindowDressing.getWindowActionGroup().activatePreviousWindow(context.event)
    choosePopup(
      context.event,
      "choose project",
      ProjectManager.getInstance().openProjects.toList(),
      getName = {
        it.basePath ?: "missing path"
      }
    ) {
      ProjectWindowAction(
        it.name,
        it.presentableUrl ?: Uni.log.fatalError { "missing presentableUrl" },
        null
      ).select(context.event)
    }

  }

}

fun ProjectWindowAction.select(e: AnActionEvent) {
  setSelected(e, true)
}

