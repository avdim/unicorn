package com.unicorn.plugin.update

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import com.unicorn.plugin.buildDistPlugins
import com.unicorn.plugin.installPlugin
import com.unicorn.plugin.performActionById
import com.unicorn.plugin.removeUniPlugin
import com.unicorn.plugin.ui.showDialog2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.avdim.mvi.APP_SCOPE
import java.io.File

fun integrationTest() {
  APP_SCOPE.launch {
    var parent: DialogPanel? = null
    parent = panel {
      row {
        label("empty panel only for progress")
      }
    }
      showDialog2(
          parent
      )
    val path = buildDistPlugins().firstOrNull()
    if (path == null) {
      testError("plugin path == null")
    }
    val installResult1 = installPlugin(File(path), parent)
    if (!installResult1) {
      testError("installResult1: $installResult1")
    }
      delay(6000)
      performActionById("UniIntegrationTestAction")//todo
    //todo launch action
      delay(9000)
    //        invokeLater {
    val removeResult1 = removeUniPlugin(parent)
    if (!removeResult1) {
      testError("removeResult1: $removeResult1")
    }
    //        }
      delay(25_000)
    val installResult2 = installPlugin(File(path), parent)
    if (!installResult2) {
      testError("installResult2: $installResult2")
    }
      delay(9000)
      removeUniPlugin(parent)
    println("INGETRATION TEST DONE")
    System.exit(0)
  }
}

fun testError(message:String) {
  println("TEST ERROR: $message")
  System.exit(1)
}
