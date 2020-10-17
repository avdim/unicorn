package com.unicorn.plugin.ui.render

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.layout.Cell
import com.unicorn.plugin.onTextChange
import org.jdesktop.swingx.combobox.ListComboBoxModel

fun Cell.textFieldCompletion(
  project: Project,
  label: String?,
  currentValue: String,
  autoCompletionVariants: List<String> = emptyList(),
  onEdit: (String) -> Unit
) {
  val tf: TextFieldWithAutoCompletion<String> =
    TextFieldWithAutoCompletion(
      project,
      TextFieldWithAutoCompletion.StringsCompletionProvider(
        autoCompletionVariants, null
      ),
      false,
      currentValue
    )
  tf.setPreferredWidth(200)
  tf.onTextChange { onEdit(it) }
  label?.let { label(it) }
  tf()
}

fun Cell.comboBoxCompletion(
  project: Project,
  label: String?,
  currentValue: String,
  autoCompletionVariants: List<String> = emptyList(),
  onEdit: (String) -> Unit
) {
  val cb = ComboBoxWithAutoCompletion(
    model = ListComboBoxModel(autoCompletionVariants),
    project = project
  )
  cb()
}
