@file:OptIn(ExperimentalComposeUiApi::class)

package com.unicorn.plugin.draw

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

val DRAW_COLORS = listOf(Color.Black, Color.Gray, Color.LightGray, Color.Red, Color(0xff00aa00), Color.Blue, Color.Yellow, Color.Magenta)

@Composable
fun ComposeDraw(curvesState: MutableState<List<Curve>>) {
  var drawColor by remember { mutableStateOf(DRAW_COLORS.first()) }
  var curves: List<Curve> by remember { curvesState }
  var currentPoints: List<Pt> by remember { mutableStateOf(listOf()) }
  var cursorPos by remember { mutableStateOf(Pt(0f, 0f)) }
  var texts: List<TextData> by remember { mutableStateOf(listOf()) }
  var selectedTextIndex: Int? by remember { mutableStateOf(null) }
  fun undo() {
    if (currentPoints.isNotEmpty()) {
      currentPoints = emptyList()
    } else {
      curves = curves.dropLast(1)
    }
  }

  val boxFocusRequester = remember { FocusRequester() }
  Box(
    Modifier.fillMaxSize()
      .onPreviewKeyEvent {
        when {
          it.isCtrlPressed && it.key == Key.Z -> {
            when (it.type) {
              KeyEventType.KeyDown -> {}
              KeyEventType.KeyUp -> {
                undo()
              }
            }
          }
          it.key == Key.T && selectedTextIndex == null -> {
            texts = texts + TextData(drawColor, "todo", cursorPos)
            selectedTextIndex = texts.lastIndex
          }
          selectedTextIndex != null && (it.key == Key.Escape /*|| it.key == Key.Enter*/) -> {
            selectedTextIndex = null
          }
        }
        false
      }
      .focusRequester(boxFocusRequester)
      .focusable()
  ) {
    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
      while (true) {
        val event = awaitPointerEventScope {
          awaitPointerEvent()
        }
        val nativeEvent = (event.mouseEvent as MouseEvent)
        val isAnyPressed = nativeEvent.modifiersEx and AnyButtonMask != 0
        val position = event.changes.first().position
        val pt = Pt(position.x, position.y)
        cursorPos = pt
        if (isAnyPressed) {
          boxFocusRequester.requestFocus()
          currentPoints = currentPoints + pt
        } else {
          if (currentPoints.isNotEmpty()) {
            curves = curves + Curve(drawColor, currentPoints)
            currentPoints = listOf()
          }
          if (event.type == PointerEventType.Scroll) {
            val scrollX = event.changes.first().scrollDelta.x
            val scrollY = event.changes.first().scrollDelta.y

            if (event.keyboardModifiers.isCtrlPressed) {//zoom
              val scale = 1 + (scrollY + scrollX) * 0.10f
              val center = cursorPos
              curves = curves.map {
                it.copy(
                  points = it.points.map { a -> center + (a - center) * scale }
                )
              }
              texts = texts.map {
                it.copy(
                  pos = center + (it.pos - center) * scale
                )
              }
            } else {
              event.keyboardModifiers.isShiftPressed//right
              val SCROLL_POWER = -30f
              val scrollOffset = Pt(scrollX, scrollY) * SCROLL_POWER
              curves = curves.map { it.copy(points = it.points.map { it + scrollOffset }) }
              texts = texts.map { it.copy(pos = it.pos + scrollOffset) }
            }
          }
        }
      }
    }) {
      (curves + Curve(drawColor, currentPoints)).forEach {
        if (it.points.size == 1) {
          drawCircle(it.color, radius = 4f, center = it.points.first().toOffset())
        } else if (it.points.size > 1) {
          drawPath(
            path = Path().apply {
              val first = it.points.first()
              moveTo(first.x, first.y)
              for (pt in it.points.drop(1)) {
                lineTo(pt.x, pt.y)
              }
            },
            color = it.color,
            style = Stroke(width = 3f)
          )
        }
      }
    }
    for (i in texts.indices) {
      val t = texts[i]
      if (selectedTextIndex == i) {
        TextField(t.text, { txt ->
          texts = texts.toMutableList().apply {
            set(i, t.copy(text = txt))
          }
        }, Modifier.offset(t.pos.x.dp, t.pos.y.dp))
      } else {
        Text(
          t.text,
          Modifier.offset(t.pos.x.dp, t.pos.y.dp).clickable {
            selectedTextIndex = i
          }
        )
      }
    }
    Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
      TxtButton("Clear all") { curves = emptyList() }
      Divider(Modifier.size(5.dp))
      TxtButton("Erase tool") {}
      TxtButton("Undo") { undo() }
      Divider(Modifier.size(5.dp))
      DRAW_COLORS.forEachIndexed { i, color ->
        val SIZE = 50f
        Canvas(Modifier.size(SIZE.dp).clickable {
          drawColor = color
        }) {
          drawRect(color, topLeft = Offset(0f, 0f), size = Size(SIZE, SIZE))
        }
      }
    }
  }
}

data class Pt(val x: Float = 0f, val y: Float = 0f)
data class Curve(val color: Color, val points: List<Pt>, val scroll: Pt = Pt())

private const val AnyButtonMask =
  InputEvent.BUTTON1_DOWN_MASK or InputEvent.BUTTON2_DOWN_MASK or InputEvent.BUTTON3_DOWN_MASK

operator fun Pt.plus(other: Pt): Pt = Pt(x + other.x, y + other.y)
operator fun Pt.minus(other: Pt): Pt = Pt(x - other.x, y - other.y)
operator fun Pt.times(scale: Float): Pt = Pt(x * scale, y * scale)
fun Pt.toOffset() = Offset(x, y)

@Composable
fun TxtButton(txt: String, onClick: () -> Unit) {
  Button(onClick = onClick) {
    Text(txt)
  }
}

fun main() {
  application {
    Window(onCloseRequest = ::exitApplication) {
      ComposeDraw(mutableStateOf(emptyList()))
    }
  }
}

data class TextData(val color: Color, val text: String, val pos: Pt, val scale: Float = 1f)
