package com.unicorn.plugin.action.cmd.misc

import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.dialog
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.unicorn.plugin.action.cmd.Command
import com.unicorn.plugin.perform
import javax.swing.JPanel
import kotlin.reflect.jvm.kotlinFunction

class DialogUiShowCase : Command {
  override fun execute() {
    val disposable = Disposer.newDisposable()
    val tabs = JBTabsFactory.createEditorTabs(ProjectManager.getInstance().defaultProject, disposable)
    tabs.presentation.setSupportsCompression(false)
    tabs.presentation.setAlphabeticalMode(true)

    val clazz = Class.forName("com.unicorn.plugin.action.cmd.misc.showcase.TestPanelsKt")
    for (declaredMethod in clazz.declaredMethods) {
      val method = declaredMethod.kotlinFunction!!
      if (method.returnType.classifier == JPanel::class && method.parameters.isEmpty()) {
        val panel = try {
          method.call() as JPanel
        } catch(e:Exception) {
          continue
        }
        val text = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, method.name)
          .replace("_", " ").capitalize().removeSuffix(" Panel")
        tabs.addTab(TabInfo(panel).setText(text))
      }
    }

    tabs.select(tabs.tabs.first(), false)

    val dialog = dialog("UI DSL Showcase", tabs.component, resizable = true)
    Disposer.register(dialog.disposable, disposable)
    dialog.showAndGet()
  }

}
