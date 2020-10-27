package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.unicorn.Uni
import com.unicorn.plugin.ui.render.fileManager
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showPanelDialog
import todo.mvi.createFileManagerMviStore

class FileManagerDialogAction : UniAction(), DumbAware {

  override fun actionPerformed(e: AnActionEvent) {
    openDialogFileManager()
  }

}

fun openDialogFileManager() {
  Uni.log.debug { "openDialogFileManager start" }
  val mviStore = createFileManagerMviStore()
  val dialog = showPanelDialog { close ->
    Uni.scope.stateFlowView(this, mviStore.stateFlow) { state ->
      fileManager(this, state, ProjectManager.getInstance().defaultProject, close) {
        mviStore.send(it)
      }
    }
  }
  Disposer.register(Uni, dialog.disposable)//todo move to showDialog
  Uni.log.debug { "openDialogFileManager end" }
}
