package com.unicorn.plugin.ui.render

import androidx.compose.desktop.ComposePanel
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.layout.LayoutBuilder
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.plugin.ui.showPanelDialog
import ru.tutu.idea.file.ConfUniFiles
import ru.tutu.idea.file.uniFiles
import java.io.File

fun showWelcomeDialog() {

  val githubDir = ConfUniFiles.GITHUB_DIR

  showPanelDialog(Uni) {
    row {
      label("welcome dialog")
    }
    if (githubDir.exists()) {
      renderWelcomeProjects(githubDir)
    }
    row {
      if(!BuildConfig.INTEGRATION_TEST) {
        cell {
          uniFiles(
            ProjectManager.getInstance().defaultProject,
            listOf(githubDir.absolutePath)
          )
        }
      }
    }
  }
}

fun LayoutBuilder.renderWelcomeProjects(
  githubDir: File
) {
  val welcomeProjects = listOf(
    "tutu/js-npm-migrate",
    "avdim/kotlin-node-js",
    "avdim/github-script",
    "ilgonmic/kotlin-ts",
  )
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
