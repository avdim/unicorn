package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.action.uniContext
import com.unicorn.plugin.suggestString
import ru.gg.lib.LibAll


class FastCommitAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val context = event.uniContext

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
