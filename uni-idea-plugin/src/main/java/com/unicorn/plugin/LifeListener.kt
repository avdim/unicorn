package com.unicorn.plugin

import com.intellij.openapi.project.Project
import com.unicorn.Uni
import com.unicorn.plugin.action.cmd.openDialogFileManager

class LifeListener : com.intellij.ide.AppLifecycleListener {

  init {
    Uni.bump()
  }

  override fun projectOpenFailed() {
    Uni.log.debug { "LifeListener.projectOpenFailed()" }
  }

  override fun appFrameCreated(commandLineArgs: MutableList<String>) {
    Uni.log.debug { "LifeListener.appFrameCreated($commandLineArgs)" }
  }

  override fun welcomeScreenDisplayed() {
    Uni.log.debug { "LifeListener.welcomeScreenDisplayed()" }
  }

  override fun projectFrameClosed() {
    Uni.log.debug { "LifeListener.projectFrameClosed()" }
  }

  override fun appStarting(projectFromCommandLine: Project?) {
    Uni.log.debug { "LifeListener.appStarting($projectFromCommandLine)" }
    if(Uni.buildConfig.OPEN_FILE_MANAGER_AT_START) {
      openDialogFileManager()
    }
  }

  override fun appClosing() {
    Uni.log.debug { "1 LifeListener.appClosing()" }
    Uni.job.cancel()
    ActionSubscription.stopSubscription()
  }

  override fun appWillBeClosed(isRestart: Boolean) {
    Uni.log.debug { "2 LifeListener.appWillBeClosed($isRestart)" }
  }

}
