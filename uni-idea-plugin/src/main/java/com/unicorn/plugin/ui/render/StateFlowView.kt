package com.unicorn.plugin.ui.render

import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBTabsImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

      withContext(Dispatchers.Main) {
        parentPanel.removeAll()
        parentPanel.add(
          panel {
            renderState(state)
            row {
              button("update inside Dialog") {

                parentPanel.findParent<JBTabsImpl>()?.let { tabs ->
                  val oldSelection = tabs.selectedInfo
                  if (oldSelection != null) {
                    GlobalScope.launch {
                      withContext(Dispatchers.Main) {
                        val tempTab = TabInfo(com.intellij.ui.layout.panel {}).setText("temp")
                        tabs.addTab(tempTab)
                        tabs.select(tempTab, true)
                        delay(100)
                        tabs.select(oldSelection, true)
                      }
                    }
                  }
                }
//              parentPanel.revalidate()
//              parentPanel.validate()
//              parentPanel.repaint()

              }
            }
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
