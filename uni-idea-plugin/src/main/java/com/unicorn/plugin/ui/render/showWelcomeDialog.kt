package com.unicorn.plugin.ui.render

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.layout.LayoutBuilder
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.plugin.ui.showPanelDialog
import ru.tutu.idea.file.uniFiles
import java.io.File

fun showWelcomeDialog() {
  val homeDir = File(System.getProperty("user.home"))
  val githubDir = homeDir.resolve("Desktop/github")//todo move to config with Linux/MacOS
  val absolutePath = if (githubDir.exists()) githubDir.absolutePath else "/"

  val welcomeProjects = listOf(
    "tutu/js-npm-migrate",
    "kotlin-node-js"
  )

  showPanelDialog(Uni) {
    row {
      label("welcome dialog")
    }
    renderWelcomeProjects(welcomeProjects, githubDir)
    row {
      if(!BuildConfig.DYNAMIC_UNLOAD) {
        cell {
          uniFiles(
            ProjectManager.getInstance().defaultProject,
            listOf(absolutePath)
          )
        }
      }
    }
  }
}

fun LayoutBuilder.renderWelcomeProjects(
  welcomeProjects: List<String>,
  githubDir: File
) {
  welcomeProjects.forEach { projectPath ->
    row {
      button(text = projectPath) {
        ProjectUtil.openOrImport(
          githubDir.resolve(projectPath).absolutePath,
          ProjectManager.getInstance().defaultProject,
          false
        )
      }
    }
  }
}
