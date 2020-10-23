package com.unicorn.plugin.update

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import com.unicorn.plugin.*
import com.unicorn.plugin.ui.showDialog2
import kotlinx.coroutines.*
import ru.avdim.mvi.APP_SCOPE
import java.io.File
import javax.swing.JComponent

fun integrationTest() {
  APP_SCOPE.launch {
    var parent: DialogPanel? = null
    parent = panel {
      row {
        label("empty panel only for progress")
      }
    }
    showDialog2(parent)

    installAndStartIntegrationTest(parent)

    delay(10_000)
    assertTrue("remove1") {
      removeUniPlugin(parent)
    }
    delay(10_000)
    installAndStartIntegrationTest(parent)
    delay(10_000)
    assertTrue("remove2") {
      removeUniPlugin(parent)
    }
    delay(3_000)
    println("INGETRATION TEST DONE")
    System.exit(0)

  }
}

suspend fun installAndStartIntegrationTest(parent: JComponent) {
  val path = buildDistPlugins().firstOrNull()
  if (path == null) {
    testError("plugin path == null")
  }

  val asyncPlugin = GlobalScope.async {
    waitPlugin("UniCorn")
  }
  assertTrue("install") {//todo assert message
    installPlugin(File(path), parent)
  }
  /**
   * Переменная classLoader должна иметь маленькую область видимости.
   * Если будет держаться ссылка, то плагин не получится динамически выгрузить.
   */
  val classLoader = asyncPlugin.await()
  fun ClassLoader.uniIntegrationTest() {
    val loadedClass: Class<*> = loadClass(/*com.package...*/"UniPluginIntegrationTest")
    loadedClass.constructors[0].newInstance()
  }
  classLoader.uniIntegrationTest()
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
