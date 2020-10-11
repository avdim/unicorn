package com.unicorn.plugin.action.cmd

import com.unicorn.plugin.performActionById

class ReloadGradleProjects() : Command {
  override fun execute() {
    performActionById("ExternalSystem.RefreshAllProjects")
  }

}
