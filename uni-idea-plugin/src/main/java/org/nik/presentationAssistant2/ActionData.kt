package org.nik.presentationAssistant2

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

data class ActionData(val actionId: String, val project: Project?, val actionText: String?)

fun ActionData(actionId: String, event: AnActionEvent) =
  ActionData(actionId, event.project, event.presentation.text)

