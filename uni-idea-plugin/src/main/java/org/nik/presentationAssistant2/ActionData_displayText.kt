package org.nik.presentationAssistant2

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.keymap.MacKeymapUtil
import java.util.LinkedHashMap

private val PARENT_GROUP_IDS = setOf("CodeCompletionGroup", "FoldingGroup", "GoToMenu", "IntroduceActionsGroup")
private fun fillParentNames(group: ActionGroup, parentName: String, parentNames: MutableMap<String, String>) {
  val actionManager = ActionManager.getInstance()
  for (item in group.getChildren(null)) {
    when (item) {
      is ActionGroup -> {
        if (!item.isPopup) fillParentNames(item, parentName, parentNames)
      }
      else -> {
        val id = actionManager.getId(item)
        if (id != null) {
          parentNames[id] = parentName
        }
      }
    }
  }
}

private val parentNames: Map<String, String> by lazy {
  val result = LinkedHashMap<String, String>()
  val actionManager = ActionManager.getInstance()
  for (groupId in PARENT_GROUP_IDS) {
    val group = actionManager.getAction(groupId)
    if (group is ActionGroup) {
      fillParentNames(group, group.getTemplatePresentation().text!!, result)
    }
  }
  return@lazy result
}

val ActionData.displayText: String
  get() {
    val parentGroupName = parentNames[actionId]
    val actionText =
      (if (parentGroupName != null) "$parentGroupName ${MacKeymapUtil.RIGHT} " else "") + (actionText
        ?: "").removeSuffix("...")
    return actionText
  }

