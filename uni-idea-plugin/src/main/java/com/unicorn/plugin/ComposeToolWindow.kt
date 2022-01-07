package com.unicorn.plugin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class ComposeToolWindow : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          ComposePanel().apply {
            this.size = Dimension(300, 300)
            setContent {
              var cursorPos by remember { mutableStateOf(Offset(40f, 40f)) }
              Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                while (true) {
                  val event = awaitPointerEventScope {
                    awaitPointerEvent()
                  }
                  val nativeEvent = (event.mouseEvent as MouseEvent)
                  val isAnyPressed = nativeEvent.modifiersEx and AnyButtonMask != 0
                  if (isAnyPressed) {
                    cursorPos = event.changes.first().position
                  }
                }
              }) {
                drawCircle(Color.Red, radius = 10f, cursorPos)
              }
            }
          },
          "",
          false
        )
      )
    }
  }
}

private const val AnyButtonMask =
  InputEvent.BUTTON1_DOWN_MASK or InputEvent.BUTTON2_DOWN_MASK or InputEvent.BUTTON3_DOWN_MASK
