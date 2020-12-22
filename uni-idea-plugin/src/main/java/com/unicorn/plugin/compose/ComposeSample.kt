package com.unicorn.plugin.compose

import androidx.compose.desktop.ComposePanel
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.Container
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame

fun helloComposePanel() = ComposePanel().apply {
  setContent {
    Column {
      Text("Hello Compose")
      Canvas(Modifier.size(100.dp, 100.dp)) {
        drawRect(
          size = Size(100f, 100f),
//          color = Color.Red,
          brush = LinearGradient(listOf(Color.Red, Color.Green), 0f, 0f, 100f, 100f, TileMode.Clamp),
        )
      }
      Text("Column")
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
