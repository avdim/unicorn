// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.unicorn.plugin.action.terminal

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContextMenuActionBase
import com.intellij.ui.content.Content
import com.intellij.ui.tabs.TabInfo
import com.jediterm.terminal.model.TerminalTextBuffer
import com.unicorn.Uni
import org.jetbrains.plugins.terminal.TerminalView
import org.jetbrains.plugins.terminal.vfs.TerminalSessionVirtualFileImpl
import java.io.File

class CopyTerminalTabToFile : ToolWindowContextMenuActionBase(), DumbAware {

  fun updateInTerminalToolWindow(e: AnActionEvent, project: Project, content: Content) {
    val terminalView = TerminalView.getInstance(project)
    val terminalWidget = TerminalView.getWidgetByContent(content)!!
  }

  fun actionPerformedInTerminalToolWindow(e: AnActionEvent, project: Project, content: Content) {
    val tabInfo = TabInfo(content.component)
      .setText(content.displayName)
    val terminalView = TerminalView.getInstance(project)
    val terminalWidget = TerminalView.getWidgetByContent(content)!!
    val terminalTextBuffer: TerminalTextBuffer = terminalWidget.terminalPanel.terminalTextBuffer
    val terminalText = terminalTextBuffer.historyBuffer.lines + terminalTextBuffer.screenLines

    val file: File = if (true) {
      File.createTempFile("console_buffer", ".txt")
    } else {
      FileUtilRt.createTempFile("console_buffer", ".txt")
    }
    file.writeText(terminalText)
    val vFile: VirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!

    if (false) {
      val fileEditor = FileEditorManager.getInstance(project).openFile(vFile, true).first()
//      val file: VirtualFile =
//        TerminalSessionVirtualFileImpl(tabInfo, terminalWidget, terminalView.terminalRunner.settingsProvider)
//      tabInfo.setObject(file)
//      file.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, java.lang.Boolean.TRUE)
//      val fileEditor = FileEditorManager.getInstance(project).openFile(file, true).first()
////      terminalWidget.listener = TerminalEditorWidgetListener(project, file)
//      terminalWidget.moveDisposable(fileEditor)
//      terminalView.detachWidgetAndRemoveContent(content)
//
//      file.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, null)
    }

  }

  final override fun update(e: AnActionEvent, toolWindow: ToolWindow, content: Content?) {
    e.presentation.text = "copy terminal buffer to file"
    val project = e.project
    if (project != null && project.terminalToolWindow == toolWindow && content != null) {

      updateInTerminalToolWindow(e, project, content)
    }
    else {
      e.presentation.isEnabledAndVisible = false
    }
  }

  final override fun actionPerformed(e: AnActionEvent, toolWindow: ToolWindow, content: Content?) {
    Uni.log.info { "CopyTerminalTabToFile.actionPerformed()" }
    val project = e.project
    if (project != null && project.terminalToolWindow == toolWindow && content != null) {
      actionPerformedInTerminalToolWindow(e, project, content)
    }
  }

}
