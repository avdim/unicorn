package com.unicorn.plugin.action.id

import com.github.parseGithubUrl
import com.github.toHttpsUrl
import com.intellij.my.file.ConfUniFiles
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.suggestString

@Suppress("ComponentNotRegistered", "unused")
class CloneGithubAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val projectUrl: String? = suggestString("clone GitHub project?", "paste github repo url")
    if (projectUrl != null) {
      val project = parseGithubUrl(projectUrl)
      val projectDir = ConfUniFiles.GITHUB_DIR.resolve(project.user).resolve(project.repo)

      fun open() {
        suggestOpenProject(projectDir.absolutePath)
      }

      if (projectDir.exists()) {
        open()
      } else {
        doClone2(repoUrl = project.toHttpsUrl(), dir = projectDir) {
          open()
        }
      }
    }
  }

}
