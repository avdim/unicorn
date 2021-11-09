package com.uni

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.mouseClickable
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.SystemColor.text

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Compose for Desktop",
    state = rememberWindowState(width = 800.dp, height = 800.dp)
  ) {
    var color by remember { mutableStateOf(Color(0, 0, 0)) }
    var mousePos by remember { mutableStateOf(Offset(100f, 100f)) }

    Canvas(
      Modifier.wrapContentSize(Alignment.Center)
        .fillMaxSize()
        .background(color = color)
        .pointerInput(Unit) {
          while (true) {
            val event = awaitPointerEventScope { awaitPointerEvent() }
            val awtEvent = event.mouseEvent

            if (event.buttons.isPrimaryPressed) {
              val point = awtEvent?.point
              if (point != null) {
                mousePos = Offset(point.x.toFloat(), point.y.toFloat())
              }
            }
            if (event.type == PointerEventType.Press) {

            }
          }
        }.pointerMoveFilter(
          onMove = {
//            mousePos = it
//            color = Color(it.x.toInt() % 256, it.y.toInt() % 256, 0)
            false
          }
        )
    ) {
      val dots = listOf(
        Offset(0f, 0f),
        Offset(0f, 100f),
        mousePos,
        Offset(100f, 0f),
      )
      drawPath(
        path = Path().apply {
          val start = dots[0]
          moveTo(start.x, start.y)
          (dots.drop(1) + dots[0]).forEach {
//                                        lineTo(it.x, it.y)
            quadraticBezierTo(50f, 50f, it.x, it.y)
          }
          close()
        },
        Color.Red,
        style = Stroke(width = 2f)
      )
    }
  }
}
