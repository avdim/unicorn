package com.unicorn.plugin.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

//todo coroutine scope life when dialog is open
fun showPanelDialog(lambda: com.intellij.ui.layout.LayoutBuilder.() -> kotlin.Unit) =
  showDialog2(
    panel {
      lambda()
    }
  )

