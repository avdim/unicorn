// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.unicorn.git4idea2.repo

import com.intellij.dvcs.repo.AbstractRepositoryManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import git4idea.GitVcs
import git4idea.rebase.GitRebaseSpec
import git4idea.repo.GitRepository
import git4idea.GitUtil

class GitRepositoryManager(
  val project: Project,
  val original: git4idea.repo.GitRepositoryManager
) : AbstractRepositoryManager<GitRepository?>(GitVcs.getInstance(project), GitUtil.DOT_GIT) {

  override fun isSyncEnabled(): Boolean {
    return original.isSyncEnabled
  }

  override fun getRepositories(): List<GitRepository> {
    return original.repositories
  }

  override fun shouldProposeSyncControl(): Boolean {
    return original.shouldProposeSyncControl()
  }

  var ongoingRebaseSpec: GitRebaseSpec?
    get() = original.ongoingRebaseSpec
    set(value) {
      original.ongoingRebaseSpec = value
    }

  fun hasOngoingRebase(): Boolean {
    return original.hasOngoingRebase()
  }

  fun getDirectSubmodules(superProject: GitRepository): Collection<GitRepository> {
    return original.getDirectSubmodules(superProject)
  }

  /**
   *
   * Sorts repositories "by dependency",
   * which means that if one repository "depends" on the other, it should be updated or pushed first.
   *
   * Currently submodule-dependency is the only one which is taken into account.
   *
   * If repositories are independent of each other, they are sorted [by path][DvcsUtil.REPOSITORY_COMPARATOR].
   */
  fun sortByDependency(repositories: Collection<GitRepository>): List<GitRepository> {
    return original.sortByDependency(repositories)
  }

  companion object {
    fun getInstance(project: Project): GitRepositoryManager {
      return GitRepositoryManager(project, ServiceManager.getService(project, git4idea.repo.GitRepositoryManager::class.java))
    }
  }
}