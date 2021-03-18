package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.util.ui.cloneDialog.VcsCloneDialog
import com.unicorn.Uni
import git4idea.checkout.GitCheckoutProvider

@Suppress("ComponentNotRegistered", "unused")
class CloneAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val project: Project = Uni.todoDefaultProject//event.getRequiredData<Project>(CommonDataKeys.PROJECT)
    val checkoutListener = ProjectLevelVcsManager.getInstance(project).compositeCheckoutListener
    val dialog: VcsCloneDialog = VcsCloneDialog.Builder(project).forVcs(GitCheckoutProvider::class.java)

    if (dialog.showAndGet()) {
      dialog.doClone(checkoutListener)
    }
  }

}
