package com.unicorn.plugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.swing.JComponent

fun showDialog(viewComponent: JComponent, width:Int = 800, height:Int = 800, parentDisposable: Disposable? = null, modal: Boolean = false): DialogWrapper {
  //todo close      { dialog?.close(DialogWrapper.CLOSE_EXIT_CODE) }
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
  dialog.setModal(modal)
  dialog.show()
  if (parentDisposable != null) {//todo not null
    Disposer.register(parentDisposable, dialog.disposable)
  }
  dialog.setSize(width, height)
  return dialog
}

fun showPanelDialog(parentDisposable: Disposable? = null, lambda: com.intellij.ui.layout.LayoutBuilder.(CoroutineScope) -> kotlin.Unit): DialogWrapper {
  val dialogJob = Job()
  val dialog = showDialog(
    panel {
      lambda(CoroutineScope(dialogJob))
    },
    parentDisposable = parentDisposable
  )
  Disposer.register(dialog.disposable, Disposable {
    dialogJob.cancel()
  })
  return dialog
}

fun showModalDialog(viewComponent: JComponent): Boolean {
  TODO("showAndGet")
//  dialog.setModal(true)//true allready set by default
//  val showAndGet = dialog.showAndGet()
//  dialog.close(23)
//  return showAndGet
}
