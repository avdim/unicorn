package com.unicorn.plugin.update

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import com.unicorn.plugin.*
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
    showDialog2(parent)
    val path = buildDistPlugins().firstOrNull()
    if (path == null) {
      testError("plugin path == null")
    }
    assertTrue("install1") {
      installPlugin(File(path), parent)
    }

    if (true) {
      delay(5_000)
      performActionById("UniPlugin.UniIntegrationTestAction")

      delay(20_000)
      assertTrue("remove1") {
        removeUniPlugin(parent)
      }
      delay(10_000)
      assertTrue("install2") {
        installPlugin(File(path), parent)
      }
      delay(9_000)
      assertTrue("remove2") {
        removeUniPlugin(parent)
      }
      delay(3_000)
      println("INGETRATION TEST DONE")
      System.exit(0)
    }
  }
}

fun testError(message: String) {
  println("TEST ERROR: $message")
  System.exit(1)
}

fun assertTrue(message: String, action: () -> Boolean) {
  val result = action()
  if (!result) {
    testError(message)
  }
}
