package com.unicorn.plugin.update

import com.intellij.openapi.project.Project
import com.unicorn.plugin.action.openUpdateUnicornDialog
import kotlinx.coroutines.launch
import ru.avdim.mvi.APP_SCOPE

class LifeListener : com.intellij.ide.AppLifecycleListener {

  init {
//    Uni.bump()
  }

  override fun appStarting(projectFromCommandLine: Project?) {
    APP_SCOPE.launch {
      openUpdateUnicornDialog()
    }
  }

  override fun appClosing() {
//    Uni.job.cancel()
  }

}
