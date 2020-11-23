package com.unicorn.plugin.action.id

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.ComposePanel
import androidx.compose.desktop.setContent
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame

val northClicks = mutableStateOf(0)
val westClicks = mutableStateOf(0)
val eastClicks = mutableStateOf(0)

@Suppress("ComponentNotRegistered")
class ComposeOfficialSample : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    // explicitly clear the application events
    AppManager.setEvents(
      onAppStart = null,
      onAppExit = null,
      onWindowsEmpty = null
    )
    SwingComposeWindow()
  }

}

fun SwingComposeWindow() {
  val window = JFrame()
  val composePanel = ComposePanel()
//  window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  window.title = "SwingComposeWindow"
  window.contentPane.add(actionButton("NORTH") { northClicks.value++ }, BorderLayout.NORTH)
  window.contentPane.add(actionButton("WEST") { westClicks.value++ }, BorderLayout.WEST)
  window.contentPane.add(actionButton("EAST", { eastClicks.value++ }), BorderLayout.EAST)
  window.contentPane.add(
    actionButton(
      text = "SOUTH/REMOVE COMPOSE",
      action = {
        window.contentPane.remove(composePanel)
      }
    ),
    BorderLayout.SOUTH
  )
  // addind ComposePanel on JFrame
  window.contentPane.add(composePanel, BorderLayout.CENTER)
  composePanel.setContent {
    ComposeContent()
  }
  window.setSize(800, 600)
  window.setVisible(true)
}

fun actionButton(text: String, action: (() -> Unit)? = null): JButton {
  val button = JButton(text)
  button.setToolTipText("Tooltip for $text button.")
  button.setPreferredSize(Dimension(100, 100))
  button.addActionListener { action?.invoke() }
  return button
}

@Composable
fun ComposeContent() {
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
