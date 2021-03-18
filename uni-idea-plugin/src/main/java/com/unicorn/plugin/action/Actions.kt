package com.unicorn.plugin.action

import bootRuntime2.main.ChooseBootRuntimeAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.unicorn.plugin.action.id.*
import com.unicorn.plugin.action.terminal.CopyTerminalTabToFile

object Actions {

  interface GroupElement {
    fun register()
    fun unregister()
    val action: AnAction
  }

  class ActionData(val id: String, override val action: AnAction) : GroupElement {

    override fun register() {
      ActionManager.getInstance().registerAction(id, action)
    }

    override fun unregister() {
      ActionManager.getInstance().unregisterAction(id)
    }

  }

  class ActionGroupData(val id: String, val name: String, val actions: List<GroupElement>) : GroupElement {

    val group = MyActionGroup(name, actions.map { it.action })
    override val action: AnAction = group

    override fun register() {
      actions.forEach {
        it.register()
      }
      ActionManager.getInstance().registerAction(id, action)
    }

    override fun unregister() {
      actions.forEach {
        it.unregister()
      }
      ActionManager.getInstance().unregisterAction(id)
    }

  }

  var actionGroup: ActionGroupData? = null

  fun register() {
    if (actionGroup == null) {
      actionGroup = ActionGroupData(
        id = "UniCorn.action-group",
        name = "uni group",
        actions = listOf(
          ActionData("com.unicorn.plugin.action.id.FileManagerToolWindowAction", FileManagerToolWindowAction()),
          ActionData("com.unicorn.plugin.action.id.WelcomeAction", WelcomeAction()),
          ActionData("com.unicorn.plugin.action.id.FileManagerDialogAction", FileManagerDialogAction()),
          ActionData("com.unicorn.plugin.action.id.ChooseProjectAction", ChooseProjectAction()),
          ActionData("com.unicorn.plugin.action.id.ChooseRuntimeAction", ChooseRuntimeAction()),
          ActionData("com.unicorn.plugin.action.id.ContextMenuAction", ContextMenuAction()),
          ActionData("com.unicorn.plugin.action.id.FastCommitAction", FastCommitAction()),
          ActionData("com.unicorn.plugin.action.id.FrameSwitchAction", FrameSwitchAction()),
          ActionData("com.unicorn.plugin.action.id.OpenFileInTerminalAction", OpenFileInTerminalAction()),
          ActionData("com.unicorn.plugin.action.id.OpenProjectAction", OpenProjectAction()),
          ActionData("com.unicorn.plugin.action.id.QuickTypeDefinitionAction", QuickTypeDefinitionAction()),
          ActionData("com.unicorn.plugin.action.id.ReloadGradleAction", ReloadGradleAction()),
          ActionData("com.unicorn.plugin.action.id.SelectInAction", SelectInAction()),
          ActionData("com.unicorn.plugin.action.id.QuickPreviewAction2", QuickPreviewAction2()),
          ActionData("com.unicorn.plugin.action.id.AesAction", AesAction()),
          ActionGroupData(
            id = "UniCorn.action-group.misc",
            name = "misc",
            actions = listOf(
              ActionData("com.unicorn.plugin.action.id.RestartAction", RestartAction()),
              ActionData("com.unicorn.plugin.action.id.ActionPopupMenuAction", ActionPopupMenuAction()),
              ActionData("com.unicorn.plugin.action.id.DialogUiShowcaseAction", DialogUiShowcaseAction()),
              ActionData("com.unicorn.plugin.action.id.KtorServerAction", KtorServerAction()),
              ActionData("com.unicorn.plugin.action.id.GetGithubTokenAction", GetGithubTokenAction()),
              ActionData("bootRuntime2.main.ChooseBootRuntimeAction", ChooseBootRuntimeAction()),
              ActionData("com.unicorn.plugin.action.id.ComposeOfficialSample", ComposeOfficialSampleAction()),
              ActionData("com.unicorn.plugin.action.id.ComposeOfficialSample2", ComposeOfficialSample2Action()),
              ActionData("com.unicorn.plugin.action.id.ComposePanelAction", ComposePanelAction()),
            )
          )
        )
      ).also {
        it.register()
      }
    }
    registerTerminalTabToFile()
  }

  val ACTION_TERMINAL_MOVE = "Terminal.MoveToEditor2"
  var copyTerminalTabToFileAction: AnAction? = null

  private fun registerTerminalTabToFile() {
    val group = ActionManager.getInstance().getAction("ToolWindowContextMenu") as DefaultActionGroup
    val action = CopyTerminalTabToFile()
    copyTerminalTabToFileAction = action
    ActionManager.getInstance().registerAction(ACTION_TERMINAL_MOVE, action)
    group.add(action)
  }

  private fun unregisterTerminalTabToFile() {
    val action = copyTerminalTabToFileAction
    if (action != null) {
      val group = ActionManager.getInstance().getAction("ToolWindowContextMenu") as DefaultActionGroup
      group.remove(action)
      ActionManager.getInstance().unregisterAction(ACTION_TERMINAL_MOVE)
      copyTerminalTabToFileAction = null
    }
  }

  fun unregister() {
    actionGroup?.unregister()
    actionGroup = null
    unregisterTerminalTabToFile()
  }

}
