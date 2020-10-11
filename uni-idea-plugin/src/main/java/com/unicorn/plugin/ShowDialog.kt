package com.unicorn.plugin

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

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

fun showModalDialog(viewComponent: JComponent): Boolean {
  TODO("showAndGet")
//  dialog.setModal(true)//true allready set by default
//  val showAndGet = dialog.showAndGet()
//  dialog.close(23)
//  return showAndGet
}

//todo coroutine scope life when dialog is open
fun showPanelDialog(lambda: com.intellij.ui.layout.LayoutBuilder.() -> kotlin.Unit) =
  showDialog(
    panel {
      lambda()
    }
  )

