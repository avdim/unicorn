package com.unicorn.plugin.action

import com.intellij.ide.plugins.PluginDescriptorLoader
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.layout.panel
import com.sample.getGithubMail
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showDialog2
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ru.avdim.mvi.APP_SCOPE
import ru.avdim.mvi.createStore
import ru.avdim.mvi.createStoreWithSideEffect
import java.io.File
import javax.swing.JComponent

data class State(
  val counter: Int = 0
)

sealed class Action {
  object Increment:Action()
}

class UpdateUnicornAction : AnAction(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = true
  }

  override fun actionPerformed(event: AnActionEvent) {

    val file = File("/Users/dim/Desktop/unicorn-0.11.0.zip")
    val descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(file.toPath(), null)

    val store = createStore/*todo WithSideEffect*/(State()) { s, a: Action ->
      when(a) {
        is Action.Increment-> {
          s.copy(
            counter = s.counter + 1
          )
        }
      }
    }

    var panelComponent: JComponent? = null
    panelComponent = panel {
      APP_SCOPE.stateFlowView(this, store.stateFlow) { state->
        row {
          button("counter ${state.counter}") {
            store.send(Action.Increment)
          }
        }
      }
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
      row {
        button("github mail") {
          getGithubMail() { mail ->
            println(mail)
          }
        }
      }
    }
    showDialog2(panelComponent)
  }

}
