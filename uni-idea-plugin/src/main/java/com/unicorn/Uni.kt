package com.unicorn

import com.intellij.openapi.vfs.VirtualFile
import com.unicorn.log.console.ConsoleLog
import com.unicorn.log.lib.Log
import com.unicorn.log.recent.RecentLog
import com.unicorn.log.tmpfile.TmpFileLog
import com.unicorn.plugin.ActionSubscription
import com.unicorn.plugin.configureIDE
import kotlinx.coroutines.*

object Uni {
  val USE_FILE_TREE_PROVIDER = false
  val log = Log
  val buildConfig = BuildConfig
  var selectedFile: VirtualFile? = null
  val job = Job()
  val scope:CoroutineScope = MainScope() + job
  val DYNAMIC_UNLOAD = true

  init {
    scope.launch {
      launch {
        RecentLog.start()
      }
      launch {
        ConsoleLog.start()
      }
      launch {
        TmpFileLog.start()
      }
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

}
