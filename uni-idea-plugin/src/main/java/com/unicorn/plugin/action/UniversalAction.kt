package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.unicorn.plugin.action.cmd.*
import com.unicorn.plugin.action.cmd.DialogFileManager
import java.awt.Component
import java.awt.KeyboardFocusManager

class UniversalContext(
  val event: AnActionEvent,
  val popupPoint: RelativePoint,
  val focusOwner: Component?
)

class UniversalAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val context = UniversalContext(
      event = event,
      popupPoint = JBPopupFactory.getInstance().guessBestPopupLocation(event.dataContext),
      focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
    )
    chooseCommand(
      event,
      UniFileManager(context.event),
      DialogFileManager(),
      OpenProject(),
      OpenFileInTerminal(context.event),
      BuildInstallPlugin(context),
      ContextMenu(context),
      FastCommit(context),
      ChooseRuntime(),
      ChooseProject(context),
      FrameSwitch(context),
      QuickTypeDefinition(context),
      ReloadGradleProjects(),
      SelectInCmd(),
      Misc(context)
    )
  }

}
