package com.unicorn.plugin

import com.intellij.ide.actions.ActivateToolWindowAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

val Project.dir: File get() = basePath?.let { File(it) }!!

fun showMessage(message: String, title: String = "") {
  MessageDialog(message, title, arrayOf<String>(), 0, null)
    .show()
}

fun toolWindowAction(toolWindowId: String, event: AnActionEvent): AnAction? {
  val actionId = ActivateToolWindowAction.getActionIdForToolWindow(toolWindowId)
  return ActionManager.getInstance().getAction(actionId)
}

fun virtualFile(path: String) = LocalFileSystem.getInstance()
  .findFileByIoFile(File(path))!!

fun Project.getToolWindow(id:String): ToolWindow? =
  ToolWindowManager.getInstance(this).getToolWindow(id)
