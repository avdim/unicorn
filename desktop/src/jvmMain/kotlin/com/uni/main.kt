package com.uni

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Compose for Desktop",
    state = rememberWindowState(width = 800.dp, height = 800.dp)
  ) {
    var points by remember { mutableStateOf(listOf<Offset>()) }

    Canvas(
      Modifier.wrapContentSize(Alignment.Center)
        .fillMaxSize()
        .pointerInput(Unit) {
          while (true) {
            val event = awaitPointerEventScope { awaitPointerEvent() }
            val awtEvent = event.mouseEvent
            if (event.buttons.isPrimaryPressed) {
              val point = awtEvent?.point
              if (point != null) {
                points = points + Offset(point.x.toFloat(), point.y.toFloat())
              }
            }
          }
        }
    ) {
      if (points.isNotEmpty()) {
        drawPath(
          path = Path().apply {
            val start = points[0]
            moveTo(start.x, start.y)
            (points.drop(1)).forEach {
              lineTo(it.x, it.y)
            }
          },
          Color.Red,
          style = Stroke(width = 2f)
        )
      }
    }
  }
}
