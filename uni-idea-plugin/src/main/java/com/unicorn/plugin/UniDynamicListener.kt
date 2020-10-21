package com.unicorn.plugin

import com.intellij.ide.plugins.DynamicPluginListener
import com.unicorn.Uni

class UniDynamicListener : DynamicPluginListener {

  companion object {
    init {
      Uni.bump()
    }
  }

}
