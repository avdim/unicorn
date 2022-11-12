package com.unicorn.plugin.ui.render

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.unicorn.plugin.onTextChange
import java.awt.Dimension

fun Cell.myTextField(str: String, hidden: Boolean = false, onTextChange: (String) -> Unit) {
  val jbTextField = if (hidden) JBPasswordField() else JBTextField()
  jbTextField.text = str
  jbTextField.onTextChange {
    onTextChange(it)
  }
  jbTextField()
}

fun Cell.myTextArea(str: String, hidden: Boolean = false, onTextChange: (String) -> Unit) {
  val jbTextArea = JBTextArea()
  jbTextArea.text = str
  jbTextArea.onTextChange {
    onTextChange(it)
  }
  jbTextArea()
}
