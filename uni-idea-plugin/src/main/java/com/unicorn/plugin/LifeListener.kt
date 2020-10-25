package com.unicorn.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.unicorn.Uni
import com.unicorn.myDispose

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
  }

  override fun appClosing() {
    Uni.log.debug { "1 LifeListener.appClosing()" }
    Uni.myDispose()
  }

  override fun appWillBeClosed(isRestart: Boolean) {
    Uni.log.debug { "2 LifeListener.appWillBeClosed($isRestart)" }
  }

}
