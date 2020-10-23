package com.unicorn.plugin.update

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.unicorn.update.BuildConfig

class MyDynamicListener : DynamicPluginListener {//todo move to dynamic subscription

  init {

  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    if (BuildConfig.INTEGRATION_TEST) {
      println("pluginLoaded, pluginDescriptor.name = ${pluginDescriptor.name}")
      if (pluginDescriptor.name == "UniCorn") {//todo move name to share
        val loader = pluginDescriptor.pluginClassLoader
        val loadedClass: Class<*> = loader.loadClass(/*com.package...*/"UniPluginIntegrationTest")
        loadedClass.constructors[0].newInstance()
      }
    }
  }

}
