package ru.tutu.git

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.sample.*
import java.io.File

class RepoTest {

  lateinit var git: TutuGit

  fun tempDir(prefix: String): File =
    if (true) {
      File(".").resolve("build").resolve("$prefix-${Math.random()}")
    } else {
      createTempDir(prefix, "")
    }

  @Before
  fun setUp() {
    git = TutuGit(sshConfig = SshAgentConfig())
  }

  @Test
  fun testCreateBranchAndRepoUpdate() {
    val dir = tempDir("clone_create_branch").also {
      println("temp dir: ${it.absolutePath}")
    }
    val gd: GitDir = git.cloneRepo(
      repoUrl = "git@github.com:avdim/repo_tool_sample_app.git",
      dir = dir
    )
    val repoData: RepoJson = parseJsonToRepoJson(dir.resolve("repo.json").readText())
    repoData.update(dir, IdRsaSshConfig())

    val TEMP_BRANCH = "temp"
    val libGitDir = GitDir(dir.resolve("lib1_repo"))
    libGitDir.newBranch(TEMP_BRANCH)
    libGitDir.contentDir.resolve("new_file.txt").writeText("new file content")
    repoData.update(dir, IdRsaSshConfig())
    Assert.assertTrue(libGitDir.branches().contains(TEMP_BRANCH))
  }

  @Test
  fun testUncomittedChanges() {
    val dir = tempDir("clone_create_branch").also {
      println("temp dir: ${it.absolutePath}")
    }
    val gd: GitDir = git.cloneRepo(
      repoUrl = "git@github.com:avdim/repo_tool_sample_app.git",
      dir = dir
    )
    val repoData: RepoJson = parseJsonToRepoJson(dir.resolve("repo.json").readText())
    repoData.update(dir, IdRsaSshConfig())
    println(dir.listFiles().toList().map { it.name }.joinToString("\n"))
    val libGitDir = GitDir(dir.resolve("lib1_repo"))
    libGitDir.contentDir.resolve("new_file.txt").writeText("new file content")
//    libGitDir.commit("new_file")
    repoData.update(dir, IdRsaSshConfig())
    Assert.assertTrue(libGitDir.branches().any { it.contains("repo_auto_commit") })
  }

  @Test
  fun testNoChanges() {
    val dir = tempDir("clone_create_branch").also {
      println("temp dir: ${it.absolutePath}")
    }
    val gd: GitDir = git.cloneRepo(
      repoUrl = "git@github.com:avdim/repo_tool_sample_app.git",
      dir = dir
    )
    val repoData: RepoJson = parseJsonToRepoJson(dir.resolve("repo.json").readText())
    repoData.update(dir, IdRsaSshConfig())
    val libGitDir = GitDir(dir.resolve("lib1_repo"))
//    libGitDir.commit("new_file")
    repoData.update(dir, IdRsaSshConfig())
    println(libGitDir.branches())
    Assert.assertFalse(libGitDir.branches().any { it.contains("repo_auto_commit") })
  }

  @Test
  fun testCreateTag() {
    val dir = tempDir("create_tag").also {
      println("temp dir: ${it.absolutePath}")
    }
    val gd: GitDir = git.cloneRepo(
      repoUrl = "git@github.com:avdim/repo_tool_sample_lib1.git",
      dir = dir
    )
    val TEST_TAG = "test_tag"
    gd.createTag(TEST_TAG)
    println(gd.tags())
    Assert.assertTrue(gd.tags().contains(TEST_TAG))
  }

}
