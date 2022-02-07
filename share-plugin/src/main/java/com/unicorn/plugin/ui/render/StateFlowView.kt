package com.unicorn.plugin.ui.render

import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBTabsImpl
import com.unicorn.plugin.onTextChange
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel

fun <T> CoroutineScope.stateFlowView(
  layoutBuilder: LayoutBuilder,
  stateFlow: StateFlow<T>,
  renderState: LayoutBuilder.(T) -> Unit
) {
  val parentPanel = JPanel(BorderLayout())
  layoutBuilder.row {
    parentPanel()
  }
  launch {
    stateFlow.collectLatest { state ->

      withContext(Dispatchers.Swing) {
        parentPanel.removeAll()
        parentPanel.add(
          panel {
            renderState(state)

            hideableRow("") {//todo workaround to mvi repaint good
              cell {
                TextFieldWithAutoCompletion(
                  null,
                  TextFieldWithAutoCompletion.StringsCompletionProvider(
                    listOf(), null
                  ),
                  false,
                  ""
                ).invoke()

//              parentPanel.revalidate()
//              parentPanel.validate()
//              parentPanel.repaint()
              }
            }//hideableRow

          }
        )
      }

    }
  }

}

inline fun <reified T : Component> Component.findParent(): T? =
  parentSequence().mapNotNull { it as? T }.firstOrNull()

fun Component.parentSequence() = sequence {
  var current = parent
  while (current != null) {
    yield(current)
    current = current.parent
  }
}
