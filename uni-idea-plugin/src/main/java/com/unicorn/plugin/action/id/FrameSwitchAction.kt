package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.perform
import com.unicorn.plugin.ui.choosePopup


class FrameSwitchAction : AnAction(), DumbAware {

  enum class Options {
    FRAME_SWITCH,
    CLOSE_PROJECTS,
    DIAGNOSTIC
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {
    choosePopup(event, "FrameSwitch", Options.values().toList(), { it.name }) {
      when (it) {
        Options.FRAME_SWITCH -> {
          ActionManager.getInstance().getAction("FrameSwitchAction").perform()
        }
        Options.CLOSE_PROJECTS -> {
          ActionManager.getInstance().getAction("CloseProjectsAction").perform()
        }
        Options.DIAGNOSTIC -> {
          ActionManager.getInstance().getAction("DiagnosticAction").perform()
        }
      }
    }

  }

}
