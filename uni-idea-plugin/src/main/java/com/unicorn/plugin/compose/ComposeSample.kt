package com.unicorn.plugin.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.Container
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame

fun helloComposePanel() = ComposePanel().apply {
  setContent {
    Column {
      Text("before canvas")
      Canvas(Modifier.size(150.dp, 150.dp)) {
        drawRect(
          topLeft = Offset.Zero,
          size = Size(100f, 100f),
//          color = Color.Red,
          brush = Brush.linearGradient(
            colors = listOf(Color.Red, Color.Green),
            start = Offset.Zero,
            end = Offset(100f, 100f),
            tileMode = TileMode.Clamp
          ),
        )
      }
      Text("after canvas")
      Canvas(Modifier) {
        val dots = listOf(
          Offset(0f, 0f),
          Offset(0f, 100f),
          Offset(100f, 100f),
          Offset(100f, 0f),
          Offset(0f, 0f),
        )
        rotate(0f, /*pivot = Offset(50f, 50f)*/) {//todo rotate 45f
          drawPath(
            path = Path().apply {
              val start = dots[0]
              moveTo(start.x, start.y)
              dots.drop(1).forEach {
//                                        lineTo(it.x, it.y)
                quadraticBezierTo(50f, 50f, it.x, it.y)
              }
              close()
            },
            Color.Red,
          )
        }
      }
    }
  }
}

fun openComposeWindow() {
  val window = JFrame()
//  window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  window.title = "SwingComposeWindow"
  window.contentPane.addComposeSample()
  window.setSize(800, 600)
  window.setVisible(true)
}

fun Container.addComposeSample() {
  val composePanel = ComposePanel()
  val northClicks = mutableStateOf(0)
  val westClicks = mutableStateOf(0)
  val eastClicks = mutableStateOf(0)
  add(actionButton("NORTH") { northClicks.value++ }, BorderLayout.NORTH)
  add(actionButton("WEST") { westClicks.value++ }, BorderLayout.WEST)
  add(actionButton("EAST", { eastClicks.value++ }), BorderLayout.EAST)
  add(
    actionButton(
      text = "SOUTH/REMOVE COMPOSE",
      action = {
        remove(composePanel)
      }
    ),
    BorderLayout.SOUTH
  )
  // addind ComposePanel on JFrame
  add(composePanel, BorderLayout.CENTER)
  composePanel.setContent {
    ComposeContent(westClicks, northClicks, eastClicks)
  }
}

fun actionButton(text: String, action: (() -> Unit)? = null): JButton {
  val button = JButton(text)
  button.setToolTipText("Tooltip for $text button.")
  button.setPreferredSize(Dimension(100, 100))
  button.addActionListener { action?.invoke() }
  return button
}

@Composable
fun ComposeContent(westClicks: MutableState<Int>, northClicks: MutableState<Int>, eastClicks: MutableState<Int>) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Row {
      Counter("West", westClicks)
      Spacer(modifier = Modifier.width(25.dp))
      Counter("North", northClicks)
      Spacer(modifier = Modifier.width(25.dp))
      Counter("East", eastClicks)
    }
  }
}

@Composable
fun Counter(text: String, counter: MutableState<Int>) {
  Surface(
    modifier = Modifier.size(130.dp, 130.dp),
    color = Color(180, 180, 180),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column {
      Box(
        modifier = Modifier.height(30.dp).fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "${text}Clicks: ${counter.value}")
      }
      Spacer(modifier = Modifier.height(25.dp))
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Button(onClick = { counter.value++ }) {
          Text(text = text, color = Color.White)
        }
      }
    }
  }
}
