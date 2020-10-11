package com.unicorn.plugin.action.cmd

import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.suggestString
import ru.gg.lib.LibAll

class FastCommit(private val context: UniversalContext) : Command {
  override fun execute() {
    val projectPath = context.event.project?.basePath

    val branchResult = LibAll.nativeCmd("git show-branch")
      .path(projectPath)
      .execute()

    val message: String? = suggestString("message", if (branchResult.success) branchResult.resultStr else "...")
    if (message != null) {
      val msg = message.replace("\n", "")
      LibAll.nativeCmd("git add .")
        .path(projectPath)
        .execute()
      Thread.sleep(10)
      LibAll.nativeCmd("git commit -m '$msg' ")
        .path(projectPath)
        .execute()
    }

  }
}
