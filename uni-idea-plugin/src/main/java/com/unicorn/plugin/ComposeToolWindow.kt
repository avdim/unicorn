package com.unicorn.plugin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.unicorn.plugin.draw.ComposeDraw
import java.awt.Dimension

class ComposeToolWindow : ToolWindowFactory, DumbAware {
  @OptIn(ExperimentalComposeUiApi::class)
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          ComposePanel().apply {
            this.size = Dimension(300, 300)
            setContent {
              ComposeDraw()
            }
          },
          "",
          false
        )
      )
    }
  }
}
