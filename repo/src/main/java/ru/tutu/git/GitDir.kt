package ru.tutu.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.RemoteConfig
import ru.tutu.log.TutuLog
import java.io.File

class GitDir constructor(val contentDir: File) {
  val metaDataDir: File? get() = contentDir.resolve(".git") //FileRepositoryBuilder().findGitDir(contentDir)?.gitDir?.normalize()
  val git: Git by lazy { Git.open(metaDataDir) }
}

fun GitDir.checkAllCommitted():Boolean {
  val status = git.status().call()
  val diff = status.uncommittedChanges + status.untracked
  if (diff.isNotEmpty()) {
    println("git dir ${contentDir.absolutePath} contains changes")
    println(diff.joinToString("\n"))
    return false
  }
  return true
}

fun GitDir.isGit():Boolean = metaDataDir?.exists() == true

val GitDir.currentBranch: Branch?
  get():Branch? = branchObjects.firstOrNull {
    it.jGitRef.name == git.repository.fullBranch
  }

val GitDir.fullBranchName get() = git.repository.fullBranch

val GitDir.branchObjects: List<Branch>
  get() = git.branchList()
    .setListMode(ListBranchCommand.ListMode.ALL)
    .call()
    .map { Branch(it) }

fun GitDir.checkout(branch: Branch) {
  git.checkout()
    .setName(branch.jGitRef.name)
    .call()
}

fun GitDir.newBranch(simpleName: String): Branch {
  val remotes: MutableList<RemoteConfig> = git.remoteList().call()
  if (remotes.any { simpleName.startsWith("${it.name}/") }) {
    TutuLog.fatalError("simpleName ($simpleName) startsWith remotes name")
  }
  if (simpleName.startsWith("remotes/")) {
    TutuLog.fatalError("simpleName ($simpleName) startsWith 'remotes/'")
  }
  val ref: Ref = git
    .branchCreate()
    .setName(simpleName)
    .call()

  return Branch(ref).also {
    checkout(it)
  }
}

fun GitDir.updateSubmodules() {
  git.submoduleInit()
    .call()

  git.submoduleUpdate()
    .setFetch(true)
    .setStrategy(MergeStrategy.RECURSIVE)
    .call()
}

fun GitDir.createTag(tagName: String) {
  git.tag()
    .setName(tagName)
    .call()
}

fun GitDir.commit(title: String) {
  git.add()
    .addFilepattern("./")
    .call()

  git.commit()
    .setAuthor("bot", "bot@mail.com")
    .setMessage(title)
    .call()
}

fun GitDir.tags(): List<String> = git.tagList().call()
  .map { it.name }
  .map { it.replace("refs/tags/", "") }
  .distinct()

fun GitDir.branches(): List<String> = git.branchList().call()
  .map { it.name }
  .map { it.replace("refs/heads/", "") }
  .distinct()

