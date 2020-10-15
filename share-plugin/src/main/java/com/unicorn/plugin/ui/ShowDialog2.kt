package com.unicorn.plugin.ui

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

fun showDialog2(viewComponent: JComponent) {
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
