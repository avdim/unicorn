package com.unicorn.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.layout.panel
import com.unicorn.Uni
import com.unicorn.plugin.action.cmd.*
import com.unicorn.plugin.action.cmd.DialogFileManager
import com.unicorn.plugin.ui.showDialog2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.KeyboardFocusManager
import kotlin.coroutines.suspendCoroutine

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
    Uni.log.debug { "UniversalAction" }
    openDialogFileManager()

//    val context = UniversalContext(
//      event = event,
//      popupPoint = JBPopupFactory.getInstance().guessBestPopupLocation(event.dataContext),
//      focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
//    )
//    chooseCommand(
//      event,
//      UniFileManager(context.event),
//      DialogFileManager(),
//      OpenProject(),
//      OpenFileInTerminal(context.event),
//      BuildInstallPlugin(context),
//      ContextMenu(context),
//      FastCommit(context),
//      ChooseRuntime(),
//      ChooseProject(context),
//      FrameSwitch(context),
//      QuickTypeDefinition(context),
//      ReloadGradleProjects(),
//      SelectInCmd(),
//      Misc(context)
//    )
  }

}
