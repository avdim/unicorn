package com.unicorn

import com.intellij.ide.projectView.ViewSettings
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
  
  object fileManagerConf2 {
    @JvmField
    val isFlattenPackages = false
    @JvmField
    val isShowMembers = false
    @JvmField
    val isHideEmptyMiddlePackages = false
    @JvmField
    val isCompactDirectories = false
    @JvmField
    val isAbbreviatePackageNames = false
    @JvmField
    val isShowLibraryContents = true
    @JvmField
    val isShowModules = false
    @JvmField
    val isFlattenModules = false
    @JvmField
    val isShowURL = true
    @JvmField
    val isFoldersAlwaysOnTop = true
    @JvmField
    val skipDirInPsiDirectoryNode = true//todo inline
    @JvmField
    val isShowVisibilityIcons = true//default false
  }
  @JvmStatic
  val fileManagerConf = object : ViewSettings {
    override fun isFlattenPackages(): Boolean = fileManagerConf2.isFlattenPackages
    override fun isShowMembers(): Boolean = fileManagerConf2.isShowMembers
    override fun isHideEmptyMiddlePackages(): Boolean = fileManagerConf2.isHideEmptyMiddlePackages
    override fun isCompactDirectories(): Boolean = fileManagerConf2.isCompactDirectories
    override fun isAbbreviatePackageNames(): Boolean = fileManagerConf2.isAbbreviatePackageNames
    override fun isShowLibraryContents(): Boolean = fileManagerConf2.isShowLibraryContents
    override fun isShowModules(): Boolean = fileManagerConf2.isShowModules
    override fun isFlattenModules(): Boolean = fileManagerConf2.isFlattenModules
    override fun isShowURL(): Boolean = fileManagerConf2.isShowURL
  }

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
