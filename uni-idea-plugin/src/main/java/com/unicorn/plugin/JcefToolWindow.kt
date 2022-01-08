package com.unicorn.plugin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.unicorn.plugin.draw.ComposeDraw
import com.unicorn.plugin.draw.Curve
import com.unicorn.plugin.draw.TextData
import java.awt.Dimension

class JcefToolWindow : ToolWindowFactory, DumbAware {
  @OptIn(ExperimentalComposeUiApi::class)
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          component,
          "",
          false
        )
      )
    }
  }

  companion object {
    val browser by lazy { JBCefBrowser("https://webwhiteboard.com/") }
    val component by lazy { browser.component }
  }

}
