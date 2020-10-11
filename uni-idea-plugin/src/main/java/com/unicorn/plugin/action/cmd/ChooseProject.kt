package com.unicorn.plugin.action.cmd

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.impl.ProjectWindowAction
import com.unicorn.Uni
import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.action.cmd.misc.ProjectWindowActionGroup2
import com.unicorn.plugin.ui.choosePopup

class ChooseProject(val context: UniversalContext) : Command {
  override fun execute() {
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

fun projectsList(project: Project?) = ProjectWindowActionGroup2().projectsList(project)
