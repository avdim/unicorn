package com.unicorn.plugin.action.id

import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.IntSize
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.compose.ComposeSizeAdjustmentWrapper
import com.jetbrains.compose.theme.WidgetTheme
import com.jetbrains.compose.widgets.*
import com.unicorn.plugin.compose.openComposeWindow
import javax.swing.JComponent

@Suppress("ComponentNotRegistered")
class ComposeOfficialSample2Action : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    // explicitly clear the application events
//    AppManager.setEvents(
//      onAppStart = null,
//      onAppExit = null,
//      onWindowsEmpty = null
//    )
    DemoDialog(event.project).show()
  }

  class DemoDialog(project: Project?) : DialogWrapper(project) {
    init {
      title = "Demo"
      init()
    }

    override fun createCenterPanel(): JComponent {
      val dialog = this
      var packed = false
      return ComposePanel().apply {
        //preferredSize = Dimension(800, 600)
        setContent {
          ComposeSizeAdjustmentWrapper(
            window = dialog,
            panel = this,
            preferredSize = IntSize(800, 600)
          ) {
            WidgetTheme(darkTheme = true) {
              Surface(modifier = Modifier.fillMaxSize()) {
                Row {
                  Column(
                    modifier = Modifier.fillMaxHeight().weight(1f)
                  ) {
                    Buttons()
                    Loaders()
                    TextInputs()
                    Toggles()
                  }
                  Box(
                    modifier = Modifier.fillMaxHeight().weight(1f)
                  ) {
                    LazyScrollable()
                  }
                }
              }
            }
          }
        }
      }
    }
  }


}

