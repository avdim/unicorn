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
import com.unicorn.Uni
import com.unicorn.plugin.draw.ComposeDraw
import com.unicorn.plugin.draw.Curve
import com.unicorn.plugin.draw.TextData
import java.awt.Dimension

private const val USE_JCEF_WEB_WHITEBOARD = false

class JcefToolWindow : ToolWindowFactory, DumbAware {
  @OptIn(ExperimentalComposeUiApi::class)
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (USE_JCEF_WEB_WHITEBOARD) {
      ApplicationManager.getApplication().invokeLater {
        toolWindow.contentManager.addContent(
          ContentFactory.SERVICE.getInstance().createContent(
            component,
            "",
            false
          )
        )
      }
    } else {
      Uni.log.error { "change USE_JCEF_WEB_WHITEBOARD" }
    }
  }

  companion object {
    val browser by lazy { JBCefBrowser("https://webwhiteboard.com/") }
    val component by lazy { browser.component }
  }

}
