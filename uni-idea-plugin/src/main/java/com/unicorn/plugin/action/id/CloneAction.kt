@file:Suppress("MissingRecentApi")

package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.cloneDialog.VcsCloneDialog
import com.unicorn.Uni
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
//import org.jetbrains.plugins.github.i18n.GithubBundle
//import org.jetbrains.plugins.github.util.GithubNotificationIdsHolder
//import org.jetbrains.plugins.github.util.GithubNotifications
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

fun doClone2(
  repoUrl: String,
  dir:File,
  project1: Project = Uni.todoDefaultProject,
  onComplete:()->Unit
) {
//  val parent: Path = Paths.get(dirStr).toAbsolutePath().parent
//  Files.createDirectories(parent)

  val parent = dir.parentFile
  parent.mkdirs()

  val destinationParent: VirtualFile? = parent.toVirtualFile()
  if (destinationParent == null) {
    //LOG.error("Clone Failed. Destination doesn't exist")
//    GithubNotifications.showError(
//      project1,
//      GithubNotificationIdsHolder.CLONE_UNABLE_TO_FIND_DESTINATION,
//      GithubBundle.message("clone.dialog.clone.failed"),
//      GithubBundle.message("clone.error.unable.to.find.dest")
//    )
    return
  }
  val directoryName = dir.name //Paths.get(dirStr).fileName.toString()
  val parentDirectory = parent.absolutePath.toString()

  GitCheckoutProvider.clone(project1, Git.getInstance(),
    object : CheckoutProvider.Listener {
      override fun directoryCheckedOut(directory: File?, vcs: VcsKey?) {

      }
      override fun checkoutCompleted() {
        onComplete()
      }
    },
    destinationParent, repoUrl, directoryName, parentDirectory)
}

fun File.toVirtualFile(lfs: LocalFileSystem = LocalFileSystem.getInstance()): VirtualFile? =
  lfs.findFileByIoFile(this) ?: lfs.refreshAndFindFileByIoFile(this)
