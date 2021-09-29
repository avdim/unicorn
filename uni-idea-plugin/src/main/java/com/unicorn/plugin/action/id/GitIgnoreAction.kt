package com.unicorn.plugin.action.id

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.unicorn.plugin.suggestString
import java.io.File

@Suppress("ComponentNotRegistered", "unused")
class GitIgnoreAction : UniAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val dirPath = suggestString("gitignore dir?", event.project?.basePath ?: "missing directory!!!")
    val textContent = """
      ###### Custom #############################
      .exclude
      
      ###### Gradle ##############################
      build
      .gradle

      ###### Idea ###############################
      .idea
      *.ipr
      *.iws
      *.iml

      ###### Android #############################
      out/
      local.properties

      ##### OS Specific ##########################
      .DS_Store
      Thumbs.db
      
    """.trimIndent()
    val gitignoreFile = File(dirPath).resolve(".gitignore")
    gitignoreFile.appendText(textContent)
  }

}
