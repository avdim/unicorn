package com.unicorn.plugin.ui.render.todo

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.layout.Row
import com.intellij.util.OpenSourceUtil
import com.intellij.util.ui.accessibility.ScreenReader
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

fun Row.checkedFilesList2(
    project: Project,
    files: List<VirtualFile>
) {
  val virtualFileList = SelectFilesDialog.VirtualFileList(
      project,
      true,
      true,
      files
  )
  virtualFileList.addKeyListener(object : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      val dataContext = DataManager.getInstance().getDataContext(virtualFileList)
      val file: VirtualFile? = dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
      if (KeyEvent.VK_SPACE == e.keyCode) {
//                        virtualFileList.lastSelectedPathComponent
        TODO("select ${file?.path}")
      }
      if (KeyEvent.VK_ENTER == e.keyCode) {
          OpenSourceUtil.openSourcesFrom(
              dataContext,
              ScreenReader.isActive()
          )
      }
      if (KeyEvent.VK_T == e.keyCode && e.isControlDown || KeyEvent.VK_F4 == e.keyCode) {
        TerminalView.getInstance(project).openTerminalIn(file)
      }
    }
  })
  (ScrollPaneFactory.createScrollPane(
      virtualFileList
  ))()
}