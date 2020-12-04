package com.unicorn.plugin.action.id

import androidx.compose.desktop.ComposePanel
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.layout.LayoutBuilder
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.unicorn.plugin.ui.showDialog

@Suppress("ComponentNotRegistered")
class ComposePanelAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val composePanel = ComposePanel()
    showDialog(composePanel)
    composePanel.setSize(400, 200)
    composePanel.setContent {
      Column {
        Text("Hello Compose")
        Text("Column")
      }
    }
  }

}

fun LayoutBuilder.renderWelcomeCompose() {
  row {
    val composePanel = ComposePanel()
    composePanel.setSize(400, 200)
    composePanel.setContent {
      Column {
        Text("Hello Compose")
        Text("Column")
      }
    }
    composePanel.invoke()
  }
}
