package todo

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsConfiguration

fun configureProjectOpenIDE(project: Project) {//todo call then project open
  VcsConfiguration.getInstance(project).RELOAD_CONTEXT//todo Restore workspace on branch switching
}
