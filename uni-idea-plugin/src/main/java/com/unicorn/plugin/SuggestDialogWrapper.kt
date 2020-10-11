package com.unicorn.plugin

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.sun.istack.Nullable
import javax.swing.JComponent

class SuggestDialogWrapper(val title2: String, suggest: String) : DialogWrapper(true) {

  var actualValue: String = suggest

  @Nullable
  override fun createCenterPanel(): JComponent? {
    return panel {
      row {
        label(title2)
      }
      row {
        textField({ actualValue }, { actualValue = it })
      }
    }
//        val dialogPanel = JPanel(
//                BorderLayout()
//        )
//        val label = JLabel("testing")
//        label.preferredSize = Dimension(100, 100)
//        dialogPanel.add(label, BorderLayout.CENTER)
//        return dialogPanel
  }

  init {
    init()
    title = title2
  }

}

fun suggestString(title: String, suggestValue: String): String? {
  val dialog = SuggestDialogWrapper(title, suggestValue)
  if (true) {
    dialog.showAndGet()
    return dialog.actualValue
  } else {
    return null
  }
}
