package com.unicorn.plugin

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.ProjectManager
import com.unicorn.Uni
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.tutu.idea.file.ConfUniFiles

class UniDynamicListener : DynamicPluginListener {

  companion object {
    init {
      Uni.bump()
    }
  }

  override fun beforePluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.beforePluginLoaded 1" }
  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.pluginLoaded 2" }
  }

  override fun checkUnloadPlugin(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.checkUnloadPlugin 1" }
  }

  override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    ActionSubscription.stopSubscription()
    Uni.job.cancel()
    //todo dynamic plugin can't unload when tool window is open
    ProjectManager.getInstance().openProjects.forEach {
      it.getToolWindow(ConfUniFiles.UNI_WINDOW_ID)?.let { window ->
        if (window.isVisible) {
          window.hide()
        }
      }
    }
    Uni.log.debug { "UniDynamicListener.beforePluginUnload 2" }
  }

  override fun pluginUnloaded(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    Uni.log.debug { "UniDynamicListener.pluginUnloaded start 3" }
    ActionSubscription.stopSubscription()
    Uni.log.debug { "UniDynamicListener.pluginUnloaded finish 3" }
  }


}
