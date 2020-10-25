package com.unicorn.plugin.action.id

import com.intellij.ide.actions.ShowPopupMenuAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ui.UIUtil
import com.unicorn.Uni
import com.unicorn.plugin.MyMouseEvent
import com.unicorn.plugin.action.uniContext
import com.unicorn.plugin.docReference
import java.awt.Component
import java.awt.event.MouseEvent


class ContextMenuAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val context = event.uniContext

    docReference<ShowPopupMenuAction> { }
    if (context.focusOwner == null) {
      Uni.log.error { "context.focusOwner == null" }
      return
    }

    val deepestComponent: Component = context.popupPoint.getPoint(context.focusOwner).let { point ->
      UIUtil.getDeepestComponentAt(context.focusOwner, point.x, point.y / 2) ?: context.focusOwner
    }
    val point = context.popupPoint.getPoint(deepestComponent)
    val mouseEvent = MyMouseEvent(
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
    deepestComponent.dispatchWhileNotConsumeToParent(mouseEvent)

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
