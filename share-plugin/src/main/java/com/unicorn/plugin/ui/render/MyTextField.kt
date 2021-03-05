package com.unicorn.plugin.ui.render

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.unicorn.plugin.onTextChange

fun Cell.myTextField(str: String, hidden: Boolean = false, onTextChange: (String) -> Unit) {
  val jbTextField = if (hidden) JBPasswordField() else JBTextField()
  jbTextField.text = str
  jbTextField.onTextChange {
    onTextChange(it)
  }
  jbTextField()
}
