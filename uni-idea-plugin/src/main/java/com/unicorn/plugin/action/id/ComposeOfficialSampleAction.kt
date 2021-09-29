package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.compose.openComposeWindow

@Suppress("ComponentNotRegistered")
class ComposeOfficialSampleAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    // explicitly clear the application events
//    AppManager.setEvents(
//      onAppStart = null,
//      onAppExit = null,
//      onWindowsEmpty = null
//    )
    openComposeWindow()
  }

}

