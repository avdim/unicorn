package org.nik.presentationAssistant2

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.unicorn.plugin.toast.Toast
import java.util.*

class ShortcutPresenter : Disposable {
  private val myCustomHideActions = setOf(
    "Tree-selectChild", "Tree-selectParent", "Tree-selectNext", "Tree-selectPrevious"
  )
  private val movingActions = setOf(
    "EditorLeft", "EditorRight", "EditorDown", "EditorUp",
    "EditorLineStart", "EditorLineEnd", "EditorPageUp", "EditorPageDown",
    "EditorPreviousWord", "EditorNextWord",
    "EditorScrollUp", "EditorScrollDown",
    "EditorTextStart", "EditorTextEnd",
    "EditorDownWithSelection", "EditorUpWithSelection",
    "EditorRightWithSelection", "EditorLeftWithSelection",
    "EditorLineStartWithSelection", "EditorLineEndWithSelection",
    "EditorPageDownWithSelection", "EditorPageUpWithSelection"
  )

  private val typingActions = setOf(
    IdeActions.ACTION_EDITOR_BACKSPACE, IdeActions.ACTION_EDITOR_ENTER,
    IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE
  )

  private val allHiddenActions = movingActions + typingActions + myCustomHideActions

  init {
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(
        AnActionListener.TOPIC,
        object : AnActionListener {
          override fun beforeActionPerformed(
            action: AnAction,
            dataContext: DataContext,
            event: AnActionEvent
          ) {
            val actionId = ActionManager.getInstance().getId(action) ?: return
            val actionData: ActionData = ActionData(actionId, event)
            if (SHOW_ALL_ACTIONS || !allHiddenActions.contains(actionId)) {
              showActionInfo(actionData)
            }
          }

          override fun beforeEditorTyping(c: Char, dataContext: DataContext) {}
        }
      )
  }

  fun showActionInfo(actionData: ActionData) {
    Toast.show(actionData)
  }

  fun disable() {
    Disposer.dispose(this)
  }

  override fun dispose() {
  }

}

fun Toast.show(actionData: ActionData) {
  val textFragments: ArrayList<String> = actionData.fragments
  val realProject = actionData.project ?: ProjectManager.getInstance().openProjects.firstOrNull()
  if (realProject != null && !realProject.isDisposed && realProject.isOpen) {
    show(realProject, textFragments)
  }
}

