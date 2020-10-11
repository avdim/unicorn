package com.unicorn.plugin.action.cmd

import com.intellij.ide.actions.ShowPopupMenuAction
import com.intellij.util.ui.UIUtil
import com.unicorn.Uni
import com.unicorn.plugin.MyMouseEvent
import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.docReference
import java.awt.Component
import java.awt.event.MouseEvent

class ContextMenu(val context: UniversalContext) : Command {
  override fun execute() {
    docReference<ShowPopupMenuAction> { }
    if (context.focusOwner == null) {
      Uni.log.error { "context.focusOwner == null" }
      return
    }

    val deepestComponent: Component = context.popupPoint.getPoint(context.focusOwner).let { point ->
      UIUtil.getDeepestComponentAt(context.focusOwner, point.x, point.y / 2) ?: context.focusOwner
    }
    val point = context.popupPoint.getPoint(deepestComponent)
    val event = MyMouseEvent(
      context.focusOwner,
      501,
      System.currentTimeMillis(),
      0,
      point.x,
      point.y,
      1,
      true,
      3
    )
    deepestComponent.dispatchWhileNotConsumeToParent(event)
  }
}

fun Component.dispatchWhileNotConsumeToParent(event: MouseEvent) {
  var cur: Component? = this
  while (cur != null) {
    cur.dispatchEvent(event)
    if (event.isConsumed) {
      break
    }
    cur = cur.parent
  }
}
