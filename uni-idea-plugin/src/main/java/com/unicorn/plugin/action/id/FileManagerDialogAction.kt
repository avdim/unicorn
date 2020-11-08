package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.layout.panel
import com.unicorn.Uni
import com.unicorn.plugin.ui.render.fileManager
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showDialog
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
  showPanelDialog(Uni) {
    Uni.scope.stateFlowView(this, mviStore.stateFlow) { state ->
      fileManager(this, state, ProjectManager.getInstance().defaultProject) {
        mviStore.send(it)
      }
    }
  }
  Uni.log.debug { "openDialogFileManager end" }
}
