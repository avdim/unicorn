package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.KeyboardFocusManager

val AnActionEvent.uniContext
  get(): UniversalContext {
    val event = this
    val context = UniversalContext(
      event = event,
      popupPoint = JBPopupFactory.getInstance().guessBestPopupLocation(event.dataContext),
      focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
    )
    return context
  }

class UniversalContext(
  val event: AnActionEvent,
  val popupPoint: RelativePoint,
  val focusOwner: Component?
)
