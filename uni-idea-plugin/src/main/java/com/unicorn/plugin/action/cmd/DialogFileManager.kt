package com.unicorn.plugin.action.cmd

import com.intellij.openapi.project.ProjectManager
import com.unicorn.Uni
import com.unicorn.plugin.ui.showPanelDialog
import com.unicorn.plugin.ui.render.fileManager
import com.unicorn.plugin.ui.render.stateFlowView
import kotlinx.coroutines.launch
import todo.mvi.createFileManagerMviStore

class DialogFileManager : Command {
  override fun execute() {
    openDialogFileManager()
  }
}

fun openDialogFileManager() {
  Uni.scope.launch {
    val mviStore = createFileManagerMviStore()
    showPanelDialog {
      stateFlowView(this, mviStore.stateFlow) { state ->
        fileManager(this, state, ProjectManager.getInstance().defaultProject) {
          launch {
            mviStore.intent(it)
          }
        }
      }
    }
  }
}
