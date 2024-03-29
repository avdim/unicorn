@file:Suppress("HardCodedStringLiteral", "UnstableApiUsage")
package com.unicorn.plugin.action.cmd.misc.showcase

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.UIBundle
import com.intellij.ui.components.*
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import com.intellij.ui.layout.*

fun labelRowShouldNotGrow(): JPanel {
  return panel {
    row("Create Android module") { CheckBox("FooBar module name foo")() }
    row("Android module name:") { JTextField("input")() }
  }
}

fun secondColumnSmallerPanel(): JPanel {
  val selectForkButton = JButton("Select Other Fork")

  val branchCombobox = ComboBox<String>()
  val diffButton = JButton("Show Diff")

  val titleTextField = JTextField()

  val panel = panel {
    row("Base fork:") {
      JComboBox<String>(arrayOf())(growX, CCFlags.pushX)
      selectForkButton(growX)
    }
    row("Base branch:") {
      branchCombobox(growX, pushX)
      diffButton(growX)
    }
    row("Title:") { titleTextField() }
    row("Description:") {
      scrollPane(JTextArea())
    }
  }

  // test scrollPane
  panel.preferredSize = Dimension(512, 256)
  return panel
}

@Suppress("unused")
fun visualPaddingsPanelOnlyComboBox(): JPanel {
  return panel {
    row("Combobox:") { JComboBox<String>(arrayOf("one", "two"))(growX) }
    row("Combobox Editable:") {
      val field = JComboBox<String>(arrayOf("one", "two"))
      field.isEditable = true
      field(growX)
    }
  }
}

@Suppress("unused")
fun visualPaddingsPanelOnlyButton(): JPanel {
  return panel {
    row("Button:") { button("label") {}.constraints(growX) }
  }
}

@Suppress("unused")
fun visualPaddingsPanelOnlyLabeledScrollPane(): JPanel {
  return panel {
    row("Description:") {
      scrollPane(JTextArea())
    }
  }
}

@Suppress("unused")
fun visualPaddingsPanelOnlyTextField(): JPanel {
  return panel {
    row("Text field:") { JTextField("text")() }
  }
}

fun fieldWithGear(): JPanel {
  return panel {
    row("Database:") {
      JTextField()()
//      gearButton()
    }
    row("Master Password:") {
      JBPasswordField()()
    }
  }
}

fun fieldWithGearWithIndent(): JPanel {
  return panel {
    row {
      row("Database:") {
        JTextField()()
//        gearButton()
      }
      row("Master Password:") {
        JBPasswordField()()
      }
    }
  }
}

fun alignFieldsInTheNestedGrid(): JPanel {
  return panel {
//    buttonGroup {
//      row {
//        RadioButton("In KeePass")()
//        row("Database:") {
//          JTextField()()
////          gearButton()
//        }
//        row("Master Password:") {
//          JBPasswordField()(comment = "Stored using weak encryption.")
//        }
//      }
//    }
  }
}

fun noteRowInTheDialog(): JPanel {
  val passwordField = JPasswordField()
  return panel {
    noteRow("Profiler requires access to the kernel-level API.\nEnter the sudo password to allow this. ")
    row("Sudo password:") { passwordField() }
    row { CheckBox(UIBundle.message("auth.remember.cb"), true)() }
    noteRow("Should be an empty row above as a gap. <a href=''>Click me</a>.") {
      System.out.println("Hello")
    }
  }
}

fun jbTextField(): JPanel {
  val passwordField = JBPasswordField()
  return panel {
    noteRow("Enter credentials for bitbucket.org")
    row("Username:") { JTextField("develar")() }
    row("Password:") { passwordField() }
    row {
      JBCheckBox(UIBundle.message("auth.remember.cb"), true)()
    }
  }
}

fun cellPanel(): JPanel {
  return panel {
    row("Repository:") {
      cell {
        ComboBox<String>()(comment = "Use File -> Settings Repository... to configure")
        JButton("Delete")()
      }
    }
    row {
      // need some pushx/grow component to test label cell grow policy if there is cell with several components
      scrollPane(JTextArea())
    }
  }
}

fun commentAndPanel(): JPanel {
  return panel {
    row("Repository:") {
      cell {
        checkBox("Auto Sync", comment = "Use File -> Settings Repository... to configure")
      }
    }
//    row {
//      panel("Foo", JScrollPane(JTextArea()))
//    }
  }
}

fun createLafTestPanel(): JPanel {
  TODO()
//  val spacing = createIntelliJSpacingConfiguration()
//  val panel = JPanel(GridLayout(0, 1, spacing.horizontalGap, spacing.verticalGap))
//  panel.add(JTextField("text"))
//  panel.add(JPasswordField("secret"))
//  panel.add(ComboBox<String>(arrayOf("one", "two")))
//
//  val field = ComboBox<String>(arrayOf("one", "two"))
//  field.isEditable = true
//  panel.add(field)
//
//  panel.add(JButton("label"))
//  panel.add(CheckBox("enabled"))
//  panel.add(JRadioButton("label"))
//  panel.add(JBIntSpinner(0, 0, 7))
//  panel.add(textFieldWithHistoryWithBrowseButton(null, "File", FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()))
//
//  return panel
}

fun withVerticalButtons(): JPanel {
  return panel {
    row {
      label("<html>Merging branch <b>foo</b> into <b>bar</b>")
    }
    row {
      scrollPane(JTextArea()).constraints(pushX)

      cell(isVerticalFlow = true) {
        button("Accept Yours") {}.constraints(growX)
        button("Accept Theirs") {}.constraints(growX)
        button("Merge ...") {}.constraints(growX)
      }
    }
  }
}

fun withSingleVerticalButton(): JPanel {
  return panel {
    row {
      label("<html>Merging branch <b>foo</b> into <b>bar</b>")
    }
    row {
      scrollPane(JTextArea()).constraints(pushX)

      cell(isVerticalFlow = true) {
        button("Merge ...") {}.constraints(growX)
      }
    }
  }
}

fun hideableRow(): JPanel {
  val dummyTextBinding = PropertyBinding({ "" }, {})

  return panel {
    row("Foo") {
      textField(dummyTextBinding)
    }
//    hideableRow("Bar") {
//      textField(dummyTextBinding)
//    }
  }
}

fun spannedCheckbox(): JPanel {
  return panel {
//    buttonGroup {
//      row {
//        RadioButton("In KeePass")()
//        row("Database:") {
//          // comment can lead to broken layout, so, test it
//          JTextField("test")(comment = "Stored using weak encryption. It is recommended to store on encrypted volume for additional security.")
//        }
//
//        row {
//          cell {
//            checkBox("Protect master password using PGP key")
//            val comboBox = ComboBox(arrayOf("Foo", "Bar"))
//            comboBox.isVisible = false
//            comboBox(growPolicy = GrowPolicy.MEDIUM_TEXT)
//          }
//        }
//      }
//
//      row {
//        RadioButton("Do not save, forget passwords after restart")()
//      }
//    }
  }
}

fun checkboxRowsWithBigComponents(): JPanel {
  return panel {
    row {
      CheckBox("Sample checkbox label")()
    }
    row {
      CheckBox("Sample checkbox label")()
    }
    row {
      CheckBox("Sample checkbox label")()
      ComboBox(DefaultComboBoxModel(arrayOf("asd", "asd")))()
    }
    row {
      CheckBox("Sample checkbox label")()
    }
    row {
      CheckBox("Sample checkbox label")()
      ComboBox(DefaultComboBoxModel(arrayOf("asd", "asd")))()
    }
    row {
      CheckBox("Sample checkbox label")()
      ComboBox(DefaultComboBoxModel(arrayOf("asd", "asd")))()
    }
    row {
      CheckBox("Sample checkbox label")()
      JBTextField()()
    }
    row {
      cell(isFullWidth = true) {
        CheckBox("Sample checkbox label")()
      }
    }
    row {
      cell(isFullWidth = true) {
        CheckBox("Sample checkbox label")()
        JBTextField()()
      }
    }
    row {
      cell(isFullWidth = true) {
        CheckBox("Sample checkbox label")()
//        comment("commentary")
      }
    }
  }
}

// titledRows is not enough to test because component align depends on comment components, so, pure titledRow must be tested
fun titledRow(): JPanel {
  return panel {
    titledRow("Remote settings") {
      row("Default notebook name:") { JTextField("")() }
      row("Spark version:") { JTextField("")() }
    }
  }
}


private data class TestOptions(var threadDumpDelay: Int, var enableLargeIndexing: Boolean, var largeIndexFilesCount: Int)


fun separatorAndComment() : JPanel {
  return panel {
    row("Label", separated = true) {
      textField({ "abc" }, {}).comment("comment")
    }
  }
}

fun rowWithIndent(): JPanel {
  return panel {
    row("Zero") {
      subRowIndent = 0
      row("Bar 0") {
      }
    }
    row("One") {
      subRowIndent = 1

      row("Bar 1") {
      }
    }
    row("Two") {
      subRowIndent = 2

      row("Bar 2") {
      }
    }
  }
}

