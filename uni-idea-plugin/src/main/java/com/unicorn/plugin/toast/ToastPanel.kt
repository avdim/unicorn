package com.unicorn.plugin.toast

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.popup.ComponentPopupBuilderImpl
import com.intellij.util.Alarm
import com.intellij.util.ui.Animator
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

private object Conf {
  enum class PopupHorizontalAlignment { LEFT, CENTER, RIGHT }
  enum class PopupVerticalAlignment { TOP, BOTTOM }

  val BACKGROUND_COLOR: Color = Color(186, 238, 186, 120)
  const val MARGIN: Int = 100
  const val FONT_SIZE = 14f
  const val HIDE_DELAY = 2 * 1000
  val HORIZONTAL_ALIGNMENT = PopupHorizontalAlignment.RIGHT
  val VERTICAL_ALIGNMENT = PopupVerticalAlignment.BOTTOM
}

class ToastPanel(project: Project, textFragments: List<String>) :
  NonOpaquePanel(BorderLayout()),
  Disposable {
  private val hint: JBPopup
  private val labelsPanel: JPanel
  private val hideAlarm = Alarm(this)
  private var animator: Animator
  private var phase = Phase.FADING_IN

  enum class Phase { FADING_IN, SHOWN, FADING_OUT, HIDDEN }

  init {
    labelsPanel = NonOpaquePanel(FlowLayout(FlowLayout.CENTER, 0, 0))
    updateLabelText(textFragments)
    background = Conf.BACKGROUND_COLOR
    isOpaque = true
    add(labelsPanel, BorderLayout.CENTER)
    val emptyBorder = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    border = emptyBorder

    hint = with(JBPopupFactory.getInstance().createComponentPopupBuilder(this, this) as ComponentPopupBuilderImpl) {
      setFocusable(false)
      setBelongsToGlobalPopupStack(false)
      setCancelKeyEnabled(false)
      setCancelCallback { phase = Phase.HIDDEN; true }
      createPopup()
    }
    hint.addListener(object : JBPopupListener {
      override fun beforeShown(lightweightWindowEvent: LightweightWindowEvent) {}
      override fun onClosed(lightweightWindowEvent: LightweightWindowEvent) {
        phase = Phase.HIDDEN
      }
    })

    animator = FadeInOutAnimator(true)
    hint.show(computeLocation(project))
    animator.resume()
  }

  private fun fadeOut() {
    if (phase != Phase.SHOWN) return
    phase = Phase.FADING_OUT
    Disposer.dispose(animator)
    animator = FadeInOutAnimator(false)
    animator.resume()
  }

  inner class FadeInOutAnimator(private val forward: Boolean) :
    Animator("Action Hint Fade In/Out", 1, 1, false, forward) {
    override fun paintNow(frame: Int, totalFrames: Int, cycle: Int) {
      if (forward && phase != Phase.FADING_IN
        || !forward && phase != Phase.FADING_OUT
      ) return
    }

    override fun paintCycleEnd() {
      if (forward) {
        showFinal()
      } else {
        close()
      }
    }
  }

  private fun getHintWindow(): Window? {
    if (hint.isDisposed) return null
    val window = SwingUtilities.windowForComponent(hint.content)
    if (window != null && window.isShowing) return window
    return null
  }

  private fun showFinal() {
    phase = Phase.SHOWN
    hideAlarm.cancelAllRequests()
    hideAlarm.addRequest({ fadeOut() }, Conf.HIDE_DELAY, ModalityState.any())
  }

  fun updateText(project: Project, textFragments: List<String>) {
    if (getHintWindow() == null) return
    labelsPanel.removeAll()
    updateLabelText(textFragments)
    hint.content.invalidate()
    hint.setLocation(computeLocation(project).screenPoint)
    hint.size = preferredSize
    hint.content.repaint()
    showFinal()
  }

  private fun computeLocation(project: Project): RelativePoint {
    val ideFrame = WindowManager.getInstance().getIdeFrame(project)!!
    val statusBarHeight = ideFrame.statusBar?.component?.height ?: 0
    val visibleRect = ideFrame.component.visibleRect
    val popupSize = preferredSize
    val x = when (Conf.HORIZONTAL_ALIGNMENT) {
      Conf.PopupHorizontalAlignment.LEFT -> {
        visibleRect.x + Conf.MARGIN
      }
      Conf.PopupHorizontalAlignment.CENTER -> {
        visibleRect.x + (visibleRect.width - popupSize.width) / 2
      }
      Conf.PopupHorizontalAlignment.RIGHT -> {
        visibleRect.x + visibleRect.width - popupSize.width - Conf.MARGIN
      }
    }
    val y = when (Conf.VERTICAL_ALIGNMENT) {
      Conf.PopupVerticalAlignment.TOP -> {
        visibleRect.y + Conf.MARGIN
      }
      Conf.PopupVerticalAlignment.BOTTOM -> {
        visibleRect.y + visibleRect.height - popupSize.height - statusBarHeight - Conf.MARGIN
      }
    }
    return RelativePoint(ideFrame.component, Point(x, y))
  }

  private fun updateLabelText(textFragments: List<String>) {
    labelsPanel.add(createLabels(textFragments))
  }

  private fun createLabels(textFragments: List<String>): JLabel =
    JLabel(textFragments.joinToString(" "))
      .apply {
        font = font.deriveFont(Conf.FONT_SIZE)
      }

  fun close() {
    Disposer.dispose(this)
  }

  override fun dispose() {
    phase = Phase.HIDDEN
    if (!hint.isDisposed) {
      hint.cancel()
    }
    Disposer.dispose(animator)
  }

  fun canBeReused(): Boolean = phase == Phase.FADING_IN || phase == Phase.SHOWN
}
