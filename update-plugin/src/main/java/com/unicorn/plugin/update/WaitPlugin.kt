package com.unicorn.plugin.update

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.suspendCancellableCoroutine

//todo move file to share

private class TempDynamicPluginListener(val pluginId: String, val callback: (ClassLoader) -> Unit) : DynamicPluginListener, Disposable {
  override fun dispose() {

  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    println("pluginLoaded, pluginDescriptor.name = ${pluginDescriptor.name}")
    if (pluginDescriptor.name == pluginId) {//todo move name to share
      val loader = pluginDescriptor.pluginClassLoader
      callback(loader)
    }
  }

}

suspend fun waitPlugin(pluginId: String): ClassLoader {
  lateinit var tempListener: TempDynamicPluginListener
  return suspendCancellableCoroutine { continuation ->
    tempListener = TempDynamicPluginListener(pluginId) { classLoader ->
      Disposer.dispose(tempListener)
      continuation.resumeWith(Result.success(classLoader))
    }
    ApplicationManager.getApplication().messageBus.connect(tempListener)
      .subscribe(DynamicPluginListener.TOPIC, tempListener)
  }
}
