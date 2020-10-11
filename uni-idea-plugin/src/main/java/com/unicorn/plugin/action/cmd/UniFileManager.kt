package com.unicorn.plugin.action.cmd

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.unicorn.plugin.action.terminal.terminalToolWindow
import com.unicorn.plugin.getToolWindow
import com.unicorn.plugin.toolWindowAction
import ru.tutu.idea.file.ConfUniFiles

class UniFileManager(val event: AnActionEvent) : Command {
  override fun available(): Boolean = true

  override fun execute() {
    event.project?.getToolWindow(ConfUniFiles.UNI_WINDOW_ID)?.let {
      if (it.isVisible) {
        it.hide()
      } else {
        it.show()
      }
    }
//        ToolWindowManager.getInstance(ProjectManager.getInstance().defaultProject)
//        WindowManager.getInstance()

//        val toolWindow = ToolWindowManager.getInstance(e.getProject()!!).getToolWindow("MyPlugin")
//        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(e.getProject()!!).console
//        val content = toolWindow.getContentManager().getFactory()
//            .createContent(consoleView.getComponent(), "MyPlugin Output", false)
//        toolWindow.getContentManager().addContent(content)
//        consoleView.print("Hello from MyPlugin!", ConsoleViewContentType.NORMAL_OUTPUT)

  }
}
