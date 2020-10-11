package com.unicorn.plugin.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.layout.panel
import com.unicorn.Uni
import com.unicorn.plugin.ui.render.fileManager
import com.unicorn.plugin.ui.render.stateFlowView
import kotlinx.coroutines.launch
import todo.mvi.createFileManagerMviStore

class UniToolWindow : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    Uni.scope.launch {
      val mviStore = createFileManagerMviStore()
      toolWindow.contentManager.removeAllContents(true)
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          panel {
            stateFlowView(this, mviStore.stateFlow) {
              fileManager(this, it, project) {
                launch {
                  mviStore.intent(it)
                }
              }
            }
          }, "", false
        )
      )
    }
  }
}
