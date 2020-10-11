package com.unicorn.plugin

import com.intellij.application.subscribe
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.impl.StripeButton
import com.unicorn.Uni
import de.halirutan.keypromoterx2.KeyPromoterAction
import org.nik.presentationAssistant2.ActionData
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent

object ActionSubscription {

  object Conf {
    val ACTION_LISTENER = false
    val EVENT_LISTENER = false
  }

  private val actionListener = object : AnActionListener {
    override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      val actionId = ActionManager.getInstance().getId(action) ?: return
      val actionData = ActionData(actionId, event)
      Uni.log.info { actionData }
    }

    override fun beforeEditorTyping(c: Char, dataContext: DataContext) {}
  }

  private var awtEventListener:AWTEventListener? = AWTEventListener { e ->
    if (e.id == MouseEvent.MOUSE_RELEASED && (e as MouseEvent).button == MouseEvent.BUTTON1) {
      if (e.getSource() is StripeButton) {
        val action = KeyPromoterAction(e)
        with(action) {
          Uni.log.debug { "$shortcut, $description" }
        }
      }
    }
  }

  var subscriptionDisposable: Disposable? = null

  fun startSubscription() {
    if (Conf.ACTION_LISTENER) {
      val sub = Disposable {}
      subscriptionDisposable = sub
      if (true) {
        ApplicationManager.getApplication().messageBus
          .connect(sub)
          .subscribe(
            AnActionListener.TOPIC,
            actionListener
          )
      } else {
        AnActionListener.TOPIC.subscribe(subscriptionDisposable, actionListener)
      }
    }

    if (Conf.EVENT_LISTENER) {
      Toolkit.getDefaultToolkit().addAWTEventListener(
        awtEventListener,
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.WINDOW_EVENT_MASK or AWTEvent.WINDOW_STATE_EVENT_MASK
      )
    }
  }

  @Synchronized
  fun stopSubscription() {
    Uni.log.debug { "stopSubscription" }
    if (Conf.ACTION_LISTENER) {
      subscriptionDisposable?.let {
        Disposer.dispose(it)
      }
      if (false) {
        ApplicationManager.getApplication().messageBus.dispose()
      }
    }
    if (Conf.EVENT_LISTENER) {
      if (awtEventListener != null) {
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener)
      }
      awtEventListener = null
    }
  }

}
