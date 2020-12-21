package com.unicorn.plugin.ui.render

import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.layout.LayoutBuilder
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.plugin.ui.showPanelDialog
import ru.tutu.idea.file.ConfUniFiles
import ru.tutu.idea.file.uniFiles
import java.io.File

val propertyGraph = PropertyGraph()
val laf get() = LafManager.getInstance()
val lafProperty = propertyGraph.graphProperty { laf.lookAndFeelReference }

fun showWelcomeDialog() {
  lafProperty.afterChange({ ref: LafManager.LafReference ->
    val newLaf = laf.findLaf(ref)
    if (laf.currentLookAndFeel == newLaf) return@afterChange
    QuickChangeLookAndFeel.switchLafAndUpdateUI(laf, newLaf, true)
  }, Uni)
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
    "avdim/aicup2020",
    "avdim/unicorn",
    "avdim/kotlin-node-js",
    "avdim/github-script",
    "tutu/js-npm-migrate",
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
