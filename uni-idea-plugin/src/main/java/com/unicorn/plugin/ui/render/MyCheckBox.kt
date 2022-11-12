package com.unicorn.plugin.ui.render

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.Cell

@Deprecated("not work switch")
fun Cell.myCheckBox(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
  val view = JBCheckBox(label, value)
  view.addActionListener {
    if (view.isSelected != value) {
      onChange(view.isSelected)
    }
  }
  view.addChangeListener {
    if (view.isSelected != value) {
      onChange(view.isSelected)
    }
  }
  view()
}
