package com.intellij.my.file

import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class TutuProjectViewToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val content = toolWindow.contentManager.factory.createContent(
      createUniFilesComponent(project, listOf("/")),
      "uni-files",
      false
    )
//  content.putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
//  content.icon = viewPane.icon
//  content.popupIcon = viewPane.icon
//  content.setPreferredFocusedComponent { viewPane.componentToFocus }
    toolWindow.contentManager.addContent(content)
  }

  override fun init(window: ToolWindow) {
    window.setIcon(IconLoader.getIcon(ApplicationInfoEx.getInstanceEx().toolWindowIconUrl))
    window.stripeTitle = "uni-files"
  }

}
