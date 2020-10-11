package org.sample

import ru.tutu.git.*
import java.io.File
import java.util.*

fun updateDir(projectRoot: File, repoDir: RepoDir, config: TutuGit.Config) {
  val dir: File = projectRoot.resolve(repoDir.dir)
  val gitDir = GitDir(dir)
  if (!gitDir.isGit()) {
    TutuGit(sshConfig = config).cloneRepo(repoUrl = repoDir.gitUri, dir = dir)
  }
  if(!gitDir.checkAllCommitted()) {
    gitDir.newBranch("repo_auto_commit_${Date()}".replace(Regex("[^a-zA-Z0-9_]"), "-"))
    gitDir.commit("repo auto commit")
//    throw Exception("uncommitted changes")
  }
  gitDir.git.fetch().call()

  if ((gitDir.tags() + gitDir.branches()).contains(repoDir.ref)) {
//    gitDir.git.branchRename().setNewName("temp_${Random.nextInt()}").call()
    gitDir.git.checkout().setName(repoDir.ref).call()
  } else {
    //todo create branch if not exists
//    gitDir.git.branchCreate().setName(repoDir.ref).call()
  }

//  val tag = gitDir.git.tag()
//  tag.name = "todo"
//  tag.call()

}

fun RepoJson.update(projectRoot:File, config:TutuGit.Config) {
  dirs.forEach {
    updateDir(projectRoot, it, config)
  }
}
