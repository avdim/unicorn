@file:OptIn(ExperimentalComposeUiApi::class)

package com.unicorn.plugin.draw

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.awt.awtEventOrNull
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

val DRAW_COLORS:List<Pair<Color, Float?>> = listOf(
  Color.Black to null,
  Color.Gray to null,
  Color.LightGray to null,
  Color.Red to null,
  Color(0xff00aa00) to null,
  Color.Blue to null,
  Color(0x66FF0000) to 28f,
  Color(0x6600FF00) to 28f,
  Color(0x660000FF) to 28f,
  Color(0x88FFFF00) to 28f,
  Color(0x66FF00FF) to 28f,
  Color(0x66000000) to 28f,
  Color(0xFFffFFff) to 28f,
)

@Composable
fun ComposeDraw(curvesState: MutableState<List<Curve>>, textsState: MutableState<List<TextData>>) {
  var drawColor by remember { mutableStateOf(DRAW_COLORS.first()) }
  var curves: List<Curve> by remember { curvesState }
  var currentPoints: List<Pt> by remember { mutableStateOf(listOf()) }
  var cursorPos by remember { mutableStateOf(Pt(0f, 0f)) }
  var texts: List<TextData> by remember { textsState }
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
            texts = texts + TextData(drawColor.first, "todo", cursorPos)
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
        val nativeEvent = (event.awtEventOrNull as? MouseEvent)
        val isAnyPressed = (nativeEvent?.modifiersEx ?: 0) and AnyButtonMask != 0
        val position = event.changes.first().position
        val pt = Pt(position.x, position.y)
        cursorPos = pt
        if (isAnyPressed) {
          boxFocusRequester.requestFocus()
          currentPoints = currentPoints + pt
        } else {
          if (currentPoints.isNotEmpty()) {
            curves = curves + Curve(drawColor.first, currentPoints, width = drawColor.second)
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
                  points = it.points.map { a -> center + (a - center) * scale },
                  width = it.width?.let { it * scale }
                )
              }
              texts = texts.map {
                it.copy(
                  pos = center + (it.pos - center) * scale,
                  scale = it.scale * scale
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
      (curves + Curve(drawColor.first, currentPoints, width = drawColor.second)).forEach { c->
        if (c.points.size == 1) {
          drawCircle(c.color, radius = c.width ?: 4f, center = c.points.first().toOffset())
        } else if (c.points.size > 1) {
          drawPath(
            path = Path().apply {
              val first = c.points.first()
              moveTo(first.x, first.y)
              for (pt in c.points.drop(1)) {
                lineTo(pt.x, pt.y)
              }
            },
            color = c.color,
            style = Stroke(width = c.width ?: 3f)
          )
        }
      }
    }
    for (i in texts.indices) {
      val t = texts[i]
      if (selectedTextIndex == i) {
        TextField(
          value = t.text,
          onValueChange = { txt ->
            texts = texts.toMutableList().apply {
              set(i, t.copy(text = txt))
            }
          },
          textStyle = TextStyle(color = t.color, fontSize = t.fontSize),
          modifier = Modifier.offset(t.pos.x.dp, t.pos.y.dp)
        )
      } else {
        var downPos: Pt? by remember(t) { mutableStateOf(null) }
        Text(
          text = t.text,
          fontSize = t.fontSize,
          color = t.color,
          modifier = Modifier.offset(t.pos.x.dp, t.pos.y.dp).clickable {
            selectedTextIndex = i
          }.pointerInput(t) {
            while(true) {
              val event = awaitPointerEventScope {
                awaitPointerEvent()
              }
              if (event.buttons.isPrimaryPressed) {
                if (downPos == null) {
                  downPos = event.awtEvent.toPt()
                }
                texts = texts.toMutableList().apply {
                  set(i, t.copy(pos = t.pos + event.awtEvent.toPt() - downPos!!))
                }
              } else {
                downPos = null
              }
            }
          }
        )
      }
    }
    Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
      TxtButton("Clear all") {
        curves = emptyList()
        texts = emptyList()
        selectedTextIndex = null
      }
      Divider(Modifier.size(5.dp))
      TxtButton("Erase tool") {}
      TxtButton("Undo") { undo() }
      Divider(Modifier.size(5.dp))
      DRAW_COLORS.forEachIndexed { i, color ->
        val SIZE = 50f
        Canvas(Modifier.size(SIZE.dp).clickable {
          drawColor = color
          val textIndex = selectedTextIndex
          if(textIndex != null) {
            texts = texts.toMutableList().apply {
              set(textIndex, texts[textIndex].copy(color = drawColor.first))
            }
          }
        }) {
          if(color.second != null) {
            drawCircle(color = Color.Black, radius = SIZE/2, center = Offset(SIZE / 2, SIZE / 2))
            drawCircle(color = Color.White, radius = SIZE/2/1.1f, center = Offset(SIZE / 2, SIZE / 2))
            drawCircle(color = color.first, radius = SIZE/2/1.1f, center = Offset(SIZE / 2, SIZE / 2))
          } else {
            drawRect(color.first, topLeft = Offset(0f, 0f), size = Size(SIZE, SIZE))
          }
        }
      }
    }
  }
}

data class Pt(val x: Float = 0f, val y: Float = 0f)
data class Curve(val color: Color, val points: List<Pt>, val scroll: Pt = Pt(), val width: Float? = null)

private const val AnyButtonMask =
  InputEvent.BUTTON1_DOWN_MASK or InputEvent.BUTTON2_DOWN_MASK or InputEvent.BUTTON3_DOWN_MASK

operator fun Pt.plus(other: Pt): Pt = Pt(x + other.x, y + other.y)
operator fun Pt.minus(other: Pt): Pt = Pt(x - other.x, y - other.y)
operator fun Pt.times(scale: Float): Pt = Pt(x * scale, y * scale)
fun Offset.toPt() = Pt(x, y)
fun Pt.toOffset() = Offset(x, y)
fun MouseEvent.toPt(): Pt = Pt(x.toFloat(),  y.toFloat())

@Composable
fun TxtButton(txt: String, onClick: () -> Unit) {
  Button(onClick = onClick) {
    Text(txt)
  }
}

fun main() {
  application {
    Window(onCloseRequest = ::exitApplication) {
      ComposeDraw(mutableStateOf(emptyList()), mutableStateOf(emptyList()))
    }
  }
}

data class TextData(val color: Color, val text: String, val pos: Pt, val scale: Double = 1.0)
val TextData.fontSize get() = (18 * Math.sqrt(scale)).sp
