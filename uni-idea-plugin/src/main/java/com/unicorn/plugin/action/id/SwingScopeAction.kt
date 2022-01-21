package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware


import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.swing.Swing
import org.pushingpixels.radiance.animation.ktx.RadianceComponent
import org.pushingpixels.radiance.animation.ktx.componentTimeline
import org.pushingpixels.radiance.animation.api.Timeline
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class ProcessResult(val resultChannel: ReceiveChannel<Int>, val job: Job)

fun processCancelable() : ProcessResult {
  val channel = Channel<Int>()
  val job = GlobalScope.launch {
    for (x in 1..5) {
      if (!isActive) {
        // This async operation has been canceled
        break
      }
      println("Sending $x " + SwingUtilities.isEventDispatchThread())
      // This is happening off the main thread
      channel.send(x)
      // Emulating long-running background processing
      delay(1000L)
    }
    // Close the channel as we're done processing
    channel.close()
  }
  return ProcessResult(channel, job)
}


@Suppress("ComponentNotRegistered", "unused")
class SwingScopeAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    GlobalScope.launch(Dispatchers.Swing) {
      val frame = JFrame()

      frame.layout = FlowLayout()

      val button = JButton("Start operation!")
      val status = JLabel("Progress")

      frame.add(button)
      frame.add(status)

      var currJob: Job? = null

      button.addActionListener {
        GlobalScope.launch(Dispatchers.Swing) {
          currJob?.cancel()

          val processResult = processCancelable()
          currJob = processResult.job

          // The next loop keeps on going as long as the channel is not closed
          for (y in processResult.resultChannel) {
            println("Processing $y " + SwingUtilities.isEventDispatchThread())

            status.text = "Progress $y"
          }
          status.text = "Done!"
        }
      }

      button.foreground = Color.blue
        button.componentTimeline {
            property(RadianceComponent.foreground from Color.blue to Color.red)
            duration = 1000
        }.playLoop(Timeline.RepeatBehavior.REVERSE)

      frame.size = Dimension(600, 400)
      frame.setLocationRelativeTo(null)
      frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

      frame.isVisible = true
    }
  }

}
