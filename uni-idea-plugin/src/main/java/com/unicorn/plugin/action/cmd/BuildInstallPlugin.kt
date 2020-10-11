package com.unicorn.plugin.action.cmd

import com.intellij.ide.actions.runAnything.RunAnythingManager
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.ide.plugins.*
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.util.Consumer
import com.unicorn.Uni
import com.unicorn.plugin.action.UniversalContext
import com.unicorn.plugin.docReference
import ru.gg.lib.LibAll
import java.io.File


class BuildInstallPlugin(val context: UniversalContext) : Command {
  override fun execute() {
    if (false) {//build gradle
//            RunAnythingCommandProvider.runCommand(
//                    context.event.project.projectFile!!,
//                    "echo 'hi command'",
//                    Executor
//            )
      RunAnythingUtil.executeMatched(DataContext {
        when (it) {
          "project" -> context.event.project
          "workDirectory" -> "/tmp"
          else -> {
            Uni.log.warning { "missing key: $it" }
            null
          }
        }
      }, "echo 'hi'")
      RunAnythingManager.getInstance(context.event.project!!)
    }
    LibAll.nativeCmd("./gradlew clean buildPlugin ")
      .path(context.event.project?.basePath)
//            .log()
      .execute()

    docReference<InstallFromDiskAction> { }
    val pluginFile = File(context.event.project?.basePath).resolve("build/distributions/")
      .listFiles()
      .first { it.extension == "zip" }
//    PluginInstaller.install(//todo?
//      InstalledPluginsTableModel(),
//      pluginFile,
//      Consumer { callbackData: PluginInstallCallbackData -> installPluginFromCallbackData(callbackData) },
//      null
//    )
  }
}