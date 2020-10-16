package ru.tutu.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.SshSessionFactory
import ru.tutu.log.TutuLog
import java.io.File

class TutuGit(sshConfig: Config? = null) {

  interface Config {
    val configSessionFactory: JschConfigSessionFactory
  }

  init {
    if (sshConfig != null) {
      SshSessionFactory.setInstance(sshConfig.configSessionFactory)
    }
  }

  fun openExistingGitDir(dir: File) = GitDir(dir).also {
    it.git.fetch().call()
  }

  fun cloneRepo(
    repoUrl: String,
    branch: String? = null,
    dir: File
  ): GitDir {
    if (!dir.exists()) {
      dir.mkdirs()
    }
    TutuLog.debug("Cloning from $repoUrl to $dir")
    Git.cloneRepository()
      .run {
        if (branch != null) {
          setBranch(branch)
        } else {
          this
        }
      }
      .setURI(repoUrl)
      .setDirectory(dir)
      .setCloneSubmodules(true)
      .call()
    return GitDir(dir)
  }
}
