package com.unicorn.plugin.action.cmd

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.unicorn.plugin.docReference
import org.jetbrains.plugins.terminal.TerminalView
import org.jetbrains.plugins.terminal.action.RevealFileInTerminalAction
import org.jetbrains.plugins.terminal.arrangement.TerminalArrangementManager
import org.jetbrains.plugins.terminal.arrangement.TerminalArrangementState

class OpenFileInTerminal(val event: AnActionEvent) : Command {
  val selectedFile = com.unicorn.Uni.selectedFile

  override fun available(): Boolean {
    return selectedFile != null
  }

  override fun execute() {
    docReference<RevealFileInTerminalAction> {}
    val project: Project = event.project ?: ProjectManager.getInstance().defaultProject
    val terminalView: TerminalView = TerminalView.getInstance(project)
    terminalView.openTerminalIn(selectedFile)

    //Currently not work
    val state: TerminalArrangementState? = TerminalArrangementManager.getInstance(project).state
    state?.myTabStates?.forEach {
      it.myCommandHistoryFileName
      it.myWorkingDirectory
    }
  }

}
