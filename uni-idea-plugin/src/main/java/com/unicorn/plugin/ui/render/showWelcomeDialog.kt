package com.unicorn.plugin.ui.render

import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.layout.LayoutBuilder
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.plugin.ui.showPanelDialog
import com.intellij.my.file.ConfUniFiles
import com.intellij.my.file.uniFiles
import com.unicorn.plugin.action.id.doClone2
import ru.avdim.mvi.ReducerResult
import ru.avdim.mvi.createStoreWithSideEffect
import java.io.File

val githubDir by lazy {
  ConfUniFiles.GITHUB_DIR
}

data class RepoData(
  val userName: String,
  val repoName: String
)

private data class RepoState(
  val onDisk: Boolean,
  val repoData: RepoData
)

fun RepoData.dir(): File =
  githubDir.resolve(userName).resolve(repoName)

private data class WelcomeDialogState(
  val listOfRepos: List<RepoState>
)

private sealed class Action {
  data class RepoCloned(val repoData: RepoData) : Action()
  data class CloeRepo(val repoData: RepoData) : Action()
}

private sealed class SideEffect {
  data class CloneRepo(val repoData: RepoData) : SideEffect()
}

fun showWelcomeDialog() {
  if (true) {
    //todo test false on Idea 2020.3
    // look and feel
    val propertyGraph = PropertyGraph()
    val laf = LafManager.getInstance()
    val lafProperty = propertyGraph.graphProperty { laf.lookAndFeelReference }
    lafProperty.afterChange({ ref: LafManager.LafReference ->
      val newLaf = laf.findLaf(ref)
      if (laf.currentLookAndFeel == newLaf) return@afterChange
      QuickChangeLookAndFeel.switchLafAndUpdateUI(laf, newLaf, true)
    }, Uni)
  }


  val initState = calcStateFromFileSystem()
  val store = createStoreWithSideEffect(
    initState,
    { store, effect: SideEffect ->
      when (effect) {
        is SideEffect.CloneRepo -> {
          val user = effect.repoData.userName
          val repo = effect.repoData.repoName
          doClone2(
            repoUrl = "https://github.com/$user/$repo.git",
            dir = effect.repoData.dir()
          ) {
            store.send(
              Action.RepoCloned(effect.repoData)
            )
          }
        }
      }

    }
  ) { state: WelcomeDialogState, action: Action ->
    when (action) {
      is Action.CloeRepo -> {
        ReducerResult(
          state,
          listOf(SideEffect.CloneRepo(action.repoData))
        )
      }
      is Action.RepoCloned -> {
        ReducerResult(
          calcStateFromFileSystem()//todo speedup
        )
      }
    }

  }

  showPanelDialog(Uni) {
    Uni.scope.stateFlowView(this, store.stateFlow) { state: WelcomeDialogState ->
      row {
        label("welcome dialog")
      }
      state.listOfRepos.forEach { repoState ->
        row {
          val repoName = repoState.repoData.repoName
          val userName = repoState.repoData.userName
          if (repoState.onDisk) {
            button(text = "open $userName/$repoName") {
              ProjectUtil.openOrImport(
                repoState.repoData.dir().absolutePath,
                ProjectManager.getInstance().defaultProject,
                false
              )
            }
          } else {
            button(text = "clone $userName/$repoName") {
              store.send(
                Action.CloeRepo(repoState.repoData)
              )
            }
          }
        }

      }
    }

    row {
      if (!BuildConfig.INTEGRATION_TEST) {
        cell {
          uniFiles(
            ProjectManager.getInstance().defaultProject,
            listOf(githubDir.absolutePath, "/tmp")
          )
        }
      }
    }
  }
}

private fun calcStateFromFileSystem(): WelcomeDialogState {
  val welcomeRepos = listOf(
    RepoData("avdim", "save"),
    RepoData("avdim", "unicorn"),
    RepoData("avdim", "aicup2020"),
    RepoData("avdim", "github-script"),
    RepoData("tutu", "android-core"),
    RepoData("JetBrains", "intellij-community")
  )
  return WelcomeDialogState(
    welcomeRepos.map {
      RepoState(
        onDisk = it.dir().exists(),
        repoData = it
      )
    }
  )
}
