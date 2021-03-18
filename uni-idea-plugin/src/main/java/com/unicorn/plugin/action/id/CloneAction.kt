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
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.util.GithubNotificationIdsHolder
import org.jetbrains.plugins.github.util.GithubNotifications
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("ComponentNotRegistered", "unused")
class CloneAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    if (false) {
      val project: Project = Uni.todoDefaultProject//event.getRequiredData<Project>(CommonDataKeys.PROJECT)
      val checkoutListener = ProjectLevelVcsManager.getInstance(project).compositeCheckoutListener
      val dialog: VcsCloneDialog = VcsCloneDialog.Builder(project).forVcs(GitCheckoutProvider::class.java)

      if (dialog.showAndGet()) {
        dialog.doClone(checkoutListener)
      }
    } else {
      doClone2(
        repoUrl = "https://github.com/avdim/save.git",
        dirStr = "/home/dim/Desktop/github2/save",//todo path
        Uni.todoDefaultProject,
        object : CheckoutProvider.Listener {
          override fun directoryCheckedOut(directory: File?, vcs: VcsKey?) {
            Uni.log.info { "directoryCheckedOut: $directory" }
          }

          override fun checkoutCompleted() {

          }
        }
      )
    }
  }

}

fun doClone2(
  repoUrl: String,
  dirStr: String,
  project1: Project,
  checkoutListener: CheckoutProvider.Listener
) {
  val parent: Path = Paths.get(dirStr).toAbsolutePath().parent
  if (!parent.toFile().exists()) {
    Files.createDirectories(parent)
  }

  val destinationParent: VirtualFile? = parent.toFile().toVirtualFile()
  if (destinationParent == null) {
    //LOG.error("Clone Failed. Destination doesn't exist")
    GithubNotifications.showError(
      project1,
      GithubNotificationIdsHolder.CLONE_UNABLE_TO_FIND_DESTINATION,
      GithubBundle.message("clone.dialog.clone.failed"),
      GithubBundle.message("clone.error.unable.to.find.dest")
    )
    return
  }
  val directoryName = Paths.get(dirStr).fileName.toString()
  val parentDirectory = parent.toAbsolutePath().toString()

  GitCheckoutProvider.clone(project1, Git.getInstance(), checkoutListener, destinationParent, repoUrl, directoryName, parentDirectory)
}

fun File.toVirtualFile(lfs: LocalFileSystem = LocalFileSystem.getInstance()): VirtualFile? =
  lfs.findFileByIoFile(this) ?: lfs.refreshAndFindFileByIoFile(this)
