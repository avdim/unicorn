package com.unicorn.plugin.action.cmd

import com.intellij.openapi.actionSystem.ActionManager
import com.unicorn.plugin.ui.choosePopup
import com.unicorn.plugin.perform

class FrameSwitch(val context: com.unicorn.plugin.action.UniversalContext) : Command {

  enum class Options {
    FRAME_SWITCH,
    CLOSE_PROJECTS,
    DIAGNOSTIC
  }

  override fun execute() {
    choosePopup(context.event, "FrameSwitch", Options.values().toList(), { it.name }) {
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

