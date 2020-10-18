package com.unicorn.plugin.action

import com.intellij.ide.plugins.PluginDescriptorLoader
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.layout.panel
import com.sample.Release
import com.sample.getGithubMail
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showDialog2
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ru.avdim.mvi.APP_SCOPE
import ru.avdim.mvi.ReducerResult
import ru.avdim.mvi.createStore
import ru.avdim.mvi.createStoreWithSideEffect
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

    val store = createUpdateStore()
    store.send(Action.LoadReleases)
//    store.send(Action.UpdateCurrent)//todo

    var panelComponent: JComponent? = null//todo simplify progress install/uninstall
    panelComponent = panel {
      APP_SCOPE.stateFlowView(this, store.stateFlow) { state ->
        state.releases?.forEach { release ->
          row {
            button("load ${release.assets.firstOrNull()?.browser_download_url}") {
              store.send(Action.StartLoading(release))
            }
          }
        }
        state.loaded?.let {loaded: Loaded ->
          row {
            button("install ${loaded.path}") {
              store.send(Action.Install(panelComponent!!))
            }
          }
        }
        row {
          button("remove") {
            store.send(Action.Remove(panelComponent!!))
          }
        }
      }
    }
    showDialog2(panelComponent)
  }

}
