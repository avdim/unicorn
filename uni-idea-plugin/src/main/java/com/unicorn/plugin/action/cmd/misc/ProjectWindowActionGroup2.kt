// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.unicorn.plugin.action.cmd.misc

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.impl.ProjectWindowAction
import com.intellij.platform.ModuleAttachProcessor
import java.util.*

/**
 * @author Bas Leijdekkers
 */
class ProjectWindowActionGroup2 : DefaultActionGroup() {
  private var latest: ProjectWindowAction? = null
  fun addProject(project: Project) {
    val projectLocation = project.presentableUrl ?: return
    val projectName =
      getProjectDisplayName(project)
    val windowAction = ProjectWindowAction(projectName, projectLocation, latest)
    val duplicateWindowActions =
      findWindowActionsWithProjectName(projectName)
    if (!duplicateWindowActions.isEmpty()) {
      for (action in duplicateWindowActions) {
        action.templatePresentation.text = FileUtil.getLocationRelativeToUserHome(action.projectLocation)
      }
      windowAction.templatePresentation.text = FileUtil.getLocationRelativeToUserHome(windowAction.projectLocation)
    }
    add(windowAction)
    latest = windowAction
  }

  fun removeProject(project: Project) {
    val windowAction = findWindowAction(project.presentableUrl) ?: return
    if (latest === windowAction) {
      val previous = latest!!.previous
      latest = if (previous !== latest) {
        previous
      } else {
        null
      }
    }
    remove(windowAction)
    val projectName =
      getProjectDisplayName(project)
    val duplicateWindowActions =
      findWindowActionsWithProjectName(projectName)
    if (duplicateWindowActions.size == 1) {
      duplicateWindowActions[0].templatePresentation.text = projectName
    }
    windowAction.dispose()
  }

  val isEnabled: Boolean
    get() = latest != null && latest!!.previous !== latest

  override fun isDumbAware(): Boolean {
    return true
  }

  fun activateNextWindow(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val windowAction = findWindowAction(project.presentableUrl) ?: return
    val next = windowAction.next
    next?.setSelected(e, true)
  }

  fun activatePreviousWindow(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val windowAction = findWindowAction(project.presentableUrl) ?: return
    val previous = windowAction.previous
    previous?.setSelected(e, true)
  }

  private fun findWindowAction(projectLocation: String?): ProjectWindowAction? {
    if (projectLocation == null) {
      return null
    }
    val children = getChildren(null)
    for (child in children) {
      if (child !is ProjectWindowAction) {
        continue
      }
      val windowAction = child
      if (projectLocation == windowAction.projectLocation) {
        return windowAction
      }
    }
    return null
  }

  fun findWindowActionsWithProjectName(projectName: String): List<ProjectWindowAction> {
    var result: MutableList<ProjectWindowAction>? = null
    val children = getChildren(null)
    for (child in children) {
      if (child !is ProjectWindowAction) {
        continue
      }
      val windowAction = child
      if (projectName == windowAction.projectName) {
        if (result == null) {
          result = ArrayList()
        }
        result.add(windowAction)
      }
    }
    return result ?: emptyList()
  }

  companion object {
    fun getProjectDisplayName(project: Project): String {
      val name = ModuleAttachProcessor.getMultiProjectDisplayName(project)
      return name ?: project.name
    }
  }

  val Project.displayName get() = getProjectDisplayName(this)
  fun projectsList(
    project: Project?
  ): List<ProjectWindowAction> {
    val windowAction = findWindowAction(project?.presentableUrl) ?: return emptyList()
    return windowAction.allPrevious() + windowAction + windowAction.allNext()
  }

}

fun ProjectWindowAction.allPrevious(): List<ProjectWindowAction> =
  previous?.let {
    listOf(it) + it.allPrevious()
  }.orEmpty()

fun ProjectWindowAction.allNext(): List<ProjectWindowAction> =
  next?.let {
    listOf(it) + it.allNext()
  }.orEmpty()

