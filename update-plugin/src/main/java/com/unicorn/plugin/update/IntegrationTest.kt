package com.unicorn.plugin.update

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
    val parent: DialogPanel = panel {
      row {
        label("empty panel only for progress")
      }
    }
    showDialog2(parent)

    installAndStartIntegrationTest("install 1", parent)
    delay(1)

    assertTrue("remove1") {
      removeUniPlugin(parent)
    }

    installAndStartIntegrationTest("install 2", parent)
    delay(1)

    assertTrue("remove2") {
      removeUniPlugin(parent)
    }

    println("INGETRATION TEST DONE")
    System.exit(0)
  }
}

suspend fun installAndStartIntegrationTest(assertMessage: String, parent: JComponent?) {
  val path = buildDistPlugins().firstOrNull()
  if (path == null) {
    testError("plugin path == null")
  }

  val asyncPlugin = GlobalScope.async {
    waitPlugin("UniCorn")
  }
  assertTrue("$assertMessage, install") {
    installPlugin(File(path), parent)
  }
  /**
   * Переменная classLoader должна иметь маленькую область видимости.
   * Если будет держаться ссылка, то плагин не получится динамически выгрузить.
   */
  val classLoader = asyncPlugin.await()
  if (false) {
//  fun ClassLoader.uniIntegrationTest() {
//    val loadedClass: Class<*> = loadClass(/*com.package...*/"UniPluginIntegrationTest")
//    loadedClass.constructors[0].newInstance()
//  }
//  classLoader.uniIntegrationTest()
  } else {
    val className = /*com.package...*/"UniPluginIntegrationTest"
    val result = suspendCancellableCoroutine<Boolean> { continuation ->
      val loadedClass: Class<*> = classLoader.loadClass(className)
      loadedClass.constructors[0].newInstance({ result: Boolean ->
        continuation.resumeWith(
          Result.success(result)
        )
      })
    }
    assertTrue("$assertMessage, init class $className") {
      result
    }
  }
  //Не надо добавлять сюлда delay, иначе classLoader не освобождается
}

fun testError(message: String) {
  println("TEST ERROR: $message")
  System.exit(1)
}

suspend fun assertTrue(message: String, action: suspend () -> Boolean) {
  val result: Boolean = action()
  if (!result) {
    testError(message)
  }
}
