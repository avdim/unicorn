package com.unicorn.plugin.action.id

import aes.Base64Str
import aes.aesDecrypt
import aes.aesEncryptToBase64
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.components.JBCheckBox
import com.unicorn.Uni
import com.unicorn.plugin.compose.helloComposePanel
import com.unicorn.plugin.draw.TxtButton
import com.unicorn.plugin.ui.render.myCheckBox
import com.unicorn.plugin.ui.render.myTextArea
import com.unicorn.plugin.ui.render.myTextField
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showDialog
import com.unicorn.plugin.ui.showPanelDialog
import ru.avdim.mvi.createStore

@Suppress("ComponentNotRegistered", "unused")
class AesAction : UniAction(), DumbAware {

  data class State(
    val hidden: Boolean = false,
    val decryptMe: Base64Str = Base64Str(""),
    val secretValue: String = "",
    val secretKey: String = "",
    val secretKeyCheck: String = "",
    val encryptedResult: Base64Str = Base64Str("")
  )

  sealed class Action {
    class SetDecryptMe(val str: String) : Action()
    class SetSecretValue(val str: String) : Action()
    class SetSecretKey(val str: String) : Action()
    class SetSecretKeyCheck(val str: String) : Action()
    object DoDecrypt : Action()
    object DoEncrypt : Action()
    object ChangeHidden : Action()
  }

  override fun actionPerformed(event: AnActionEvent) {
    val store = createStore(State()) { s: State, a: Action ->
      when (a) {
        is Action.SetDecryptMe -> {
          s.copy(
            decryptMe = Base64Str(a.str)
          )
        }
        is Action.SetSecretValue -> {
          s.copy(
            secretValue = a.str
          )
        }
        is Action.SetSecretKey -> {
          s.copy(
            secretKey = a.str
          )
        }
        is Action.SetSecretKeyCheck -> {
          s.copy(
            secretKeyCheck = a.str
          )
        }
        is Action.DoDecrypt -> {
          s.copy(
            secretValue = s.decryptMe.aesDecrypt(s.secretKey)
          )
        }
        is Action.DoEncrypt -> {
          s.copy(
            encryptedResult = if (s.secretKey == s.secretKeyCheck) s.secretValue.aesEncryptToBase64(s.secretKey) else Base64Str("secretKey no checked")
          )
        }
        is Action.ChangeHidden -> {
          s.copy(
            hidden = s.hidden.not()
          )
        }
      }
    }
    val dialog = showDialog(ComposePanel().apply {
      setContent {
        val s by store.stateFlow.collectAsState()
        Column {
          Text("AES")
          Row {
            Text("secretKey:")
            MyTextField(s.secretKey, s.hidden) {
              store.send(Action.SetSecretKey(it))
            }
          }
          Row {
            Text("check secretKey:")
            MyTextField(s.secretKeyCheck, s.hidden) {
              store.send(Action.SetSecretKeyCheck(it))
            }
          }
          Row {
            TxtButton("decrypt") {
              store.send(Action.DoDecrypt)
            }
            Text("decryptMe:")
            MyTextField(s.decryptMe.str, false) {
              store.send(Action.SetDecryptMe(it))
            }
          }
          Row {
            TxtButton("encrypt") {
              store.send(Action.DoEncrypt)
            }
            Text("secret:")
            MyTextArea(s.secretValue, s.hidden) {
              store.send(Action.SetSecretValue(it))
            }
          }
          Row {
            Text("encryptedResult:")
            MyTextField("u" + "n" + "i" + "-" + "cry" + "pt" + ":" + s.encryptedResult.str) {

            }
          }
          Row {
            MyCheckBox("hidden", s.hidden) {
              store.send(Action.ChangeHidden)
            }
          }
        }
      }
    })
    dialog.setSize(800, 600)
  }

}

@Composable
fun MyTextField(value: String, hidden: Boolean = false, onChange: (String)->Unit) {
  //todo hidden
  TextField(value, onValueChange = { onChange(it) })
}

@Composable
fun MyTextArea(value: String, hidden: Boolean, onChange: (String)->Unit) {
  //todo TextArea
  //todo hidden
  TextField(value, onValueChange = { onChange(it) })
}

@Composable
fun MyCheckBox(label:String, checked:Boolean, onChange:()->Unit) {
  Checkbox(checked = checked, onCheckedChange = { onChange() })
  Text(text = label, modifier = Modifier.clickable { onChange() })
}
