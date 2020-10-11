package com.unicorn.plugin.ui

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.awt.RelativePoint
import com.unicorn.plugin.checkAndroidStudio
import java.awt.event.MouseEvent

fun <T> choosePopup(
  event: AnActionEvent,
  title: String,
  values: List<T>,
  getName: (T) -> String = { it.toString() },
  onSelected: (T) -> Unit
) {
  chooseActionInPopup(
    event,
    title,
    values.map { value ->
      object : IPopupAction {
        override fun execute(e: AnActionEvent) {
          onSelected(value)
        }

        override val text: String get() = getName(value)
      }
    }
  )
}

interface IPopupAction {
  fun execute(e: AnActionEvent)
  val text: String
}

fun chooseActionInPopup(
  event: AnActionEvent,
  title: String,
  values: List<IPopupAction>,
  mouseEvent: MouseEvent? = null
) {
  val isAs = checkAndroidStudio()
  if(isAs) {
    choosePopupOld(event, title, values, {it.text}, {it.execute(event)})
  } else {
    val popup: ListPopup = JBPopupFactory.getInstance().createActionGroupPopup(
      title,
      createActionGroup(values), event.dataContext,
      JBPopupFactory.ActionSelectionAid.NUMBERING, false
    )
    if(mouseEvent != null) {
      popup.show(RelativePoint(mouseEvent))
    } else {
      popup.showInFocusCenter()
    }
  }
}

private fun createActionGroup(
  values: List<IPopupAction>
): ActionGroup {
  return DefaultActionGroup(
    values.map { element: IPopupAction ->
      val text = element.text
      object : DumbAwareAction(text, text, null) {
        override fun actionPerformed(e: AnActionEvent) {
          element.execute(e)
        }
      }
    }.toMutableList()
  )
}

class WrapperWithName<T>(val value:T, val name:String) {
  override fun toString(): String {
    return name
  }
}

fun <T> choosePopupOld(
  event: AnActionEvent,
  title: String,
  values: List<T>,
  getName: (T) -> String = { it.toString() },
  onSelected: (T) -> Unit
) {
  var popup: ListPopup
  popup = JBPopupFactory.getInstance().createListPopup(
    object : BaseListPopupStep<WrapperWithName<T>>(
      title,
      values.map { WrapperWithName<T>(it, getName(it)) }
    ) {
      override fun isSpeedSearchEnabled(): Boolean = true
      override fun onChosen(selectedValue: WrapperWithName<T>?, finalChoice: Boolean): PopupStep<*>? {
        if (/*finalChoice && */selectedValue != null) {
          onSelected(selectedValue.value)
        }
        //                        return super.onChosen(selectedValue, finalChoice)
        return PopupStep.FINAL_CHOICE
        //todo close popup
      }

      override fun isMnemonicsNavigationEnabled(): Boolean {
        return true
      }
    },
    20
  )
  popup.showInFocusCenter()
}
