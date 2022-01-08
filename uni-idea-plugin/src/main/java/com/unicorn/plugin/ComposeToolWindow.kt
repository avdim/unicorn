package com.unicorn.plugin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
import java.util.function.UnaryOperator

data class Pt(val x: Float=0f, val y: Float=0f)
private data class Curve(val color: Int, val points: List<Pt>, val scroll:Pt = Pt())

class ComposeToolWindow : ToolWindowFactory, DumbAware {
  @OptIn(ExperimentalComposeUiApi::class)
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          ComposePanel().apply {
            this.size = Dimension(300, 300)
            setContent {
              var color by remember { mutableStateOf(0xff00aa00.toInt()) }
              var curves: List<Curve> by remember { mutableStateOf(emptyList()) }
              var currentPoints: List<Pt> by remember { mutableStateOf(listOf()) }
              var cursorPos by remember { mutableStateOf(Offset(40f, 40f)) }
              Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                while (true) {
                  val event = awaitPointerEventScope {
                    awaitPointerEvent()
                  }
                  val nativeEvent = (event.mouseEvent as MouseEvent)
                  val isAnyPressed = nativeEvent.modifiersEx and AnyButtonMask != 0
                  if (isAnyPressed) {
                    val position = event.changes.first().position
                    cursorPos = position
                    currentPoints = currentPoints + Pt(position.x, position.y)
                  } else {
                    if (currentPoints.isNotEmpty()) {
                      curves = curves + Curve(color, currentPoints)
                      currentPoints = listOf()
                    }
                    if (event.type == PointerEventType.Scroll) {
                      val SCROLL_POWER = -30f
                      val scrollX = event.changes.first().scrollDelta.x
                      val scrollY = event.changes.first().scrollDelta.y

                      val scrollOffset = Pt(scrollX, scrollY) * SCROLL_POWER
                      curves = curves.map { it.copy(points = it.points.map { it + scrollOffset }) }

                      event.keyboardModifiers.isShiftPressed//right
                      event.keyboardModifiers.isCtrlPressed//zoom
                    }
                  }
                }
              }) {
                (curves + Curve(color, currentPoints)).forEach {
                  if(it.points.size == 1) {
                    drawCircle(Color(it.color), radius = 2f, center = it.points.first().toOffset())
                  } else if(it.points.size > 1) {
                    drawPath(
                      path = Path().apply {
                        val first = it.points.first()
                        moveTo(first.x, first.y)
                        for(pt in it.points.drop(1)) {
                          lineTo(pt.x, pt.y)
                        }
                      },
                      color = Color(it.color),
                      style = Stroke(width = 2f)
                    )

                  }
                }
                drawCircle(Color.Red.copy(alpha = 0.5f), radius = 5f, cursorPos)
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

operator fun Pt.plus(other:Pt):Pt = Pt(x + other.x, y + other.y)
operator fun Pt.times(scale: Float): Pt = Pt(x * scale, y * scale)
fun Pt.toOffset() = Offset(x, y)
