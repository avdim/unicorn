package com.unicorn.plugin.action

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.ide.plugins.PluginDescriptorLoader
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.swing.JComponent

class UpdateUnicornAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {

    val file = File("/Users/dim/Desktop/unicorn-0.11.0.zip")
    val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)

    var panelComponent: JComponent? = null
    panelComponent = panel {
      row {
        button("install") {
          PluginInstaller.installAndLoadDynamicPlugin(
            file.toPath(),
            panelComponent,
            descriptor
          )
        }
      }
      row {
        button("remove") {
          PluginInstaller.uninstallDynamicPlugin(
            panelComponent,
            descriptor,
            true
          )
        }
      }
      row {
        button("progress") {
          ProgressManager.getInstance().runProcess(
            {
              runBlocking {
                repeat(10) {
                  println(it)
                  delay(200)
                }
              }
            },
            ProgressIndicatorProvider.getGlobalProgressIndicator()//DaemonProgressIndicator()
          )
        }
      }
    }
    showDialog(panelComponent)
  }

}

fun showDialog(viewComponent: JComponent) {
  val dialog = object : DialogWrapper(
    null,
    null,
    true,
    IdeModalityType.MODELESS
  ) {
    init {
      init()
    }

    override fun createCenterPanel(): JComponent {
      return viewComponent
    }

    override fun getPreferredFocusedComponent(): JComponent? {
      return super.getPreferredFocusedComponent()//todo
    }
  }
  dialog.setModal(false)
  dialog.show()
}
