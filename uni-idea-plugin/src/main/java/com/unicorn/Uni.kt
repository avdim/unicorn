package com.unicorn

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.unicorn.log.console.ConsoleLog
import com.unicorn.log.lib.Log
import com.unicorn.log.recent.RecentLog
import com.unicorn.log.tmpfile.TmpFileLog
import com.unicorn.plugin.ActionSubscription
import com.unicorn.plugin.action.Actions
import com.unicorn.plugin.configureIDE
import com.unicorn.plugin.getToolWindow
import kotlinx.coroutines.*
import com.intellij.my.file.ConfUniFiles
import com.intellij.openapi.application.impl.CoroutineExceptionHandlerImpl
import com.intellij.openapi.project.Project

object Uni : Disposable {
  val BOLD_DIRS = true
  @JvmStatic
  val log = Log
  @JvmStatic
  val todoDefaultProject by lazy { ProjectManager.getInstance().defaultProject }//todo
  @JvmStatic
  fun todoUseOpenedProject(project: Project) = project
  val buildConfig = BuildConfig
  var selectedFile: VirtualFile? = null
  val job = Job()
  val scope: CoroutineScope = MainScope() + job + CoroutineExceptionHandler { coroutineContext, throwable ->
    throwable.printStackTrace()
    Uni.log.error { "coroutine exception in coroutineContext: $coroutineContext, throwable: $throwable" }
  }
  val PLUGIN_NAME = "UniCorn"

  init {
    RecentLog.start()
    ConsoleLog.start()
    TmpFileLog.start()
    scope.launch {
      launch {
        configureIDE()
      }
      launch {
        ActionSubscription.startSubscription()
      }
    }
  }

  fun bump() {
    //do nothing, just to init
  }

  override fun dispose() {
    Uni.log.info { "Uni.dispose()" }
    ActionSubscription.stopSubscription()
    job.cancel()
    if (false) {//todo move disposable to ToolWindow
      ProjectManager.getInstance().openProjects.forEach {
        it.getToolWindow(ConfUniFiles.UNI_WINDOW_ID)?.let { window ->
          if (window.isVisible) {
            window.hide()
          }
        }
      }
    }
    Uni.log.debug { "Uni.dispose() complete" }
    Actions.unregister()
  }

}

fun Disposable.myDispose() {
  Disposer.dispose(this)
}
