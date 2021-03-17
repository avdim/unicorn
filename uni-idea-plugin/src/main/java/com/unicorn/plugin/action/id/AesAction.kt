package com.unicorn.plugin.action.id

import aes.Base64Str
import aes.aesDecrypt
import aes.aesEncryptToBase64
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.components.JBCheckBox
import com.unicorn.Uni
import com.unicorn.plugin.ui.render.myCheckBox
import com.unicorn.plugin.ui.render.myTextArea
import com.unicorn.plugin.ui.render.myTextField
import com.unicorn.plugin.ui.render.stateFlowView
import com.unicorn.plugin.ui.showPanelDialog
import ru.avdim.mvi.createStore

@Suppress("ComponentNotRegistered", "unused")
class AesAction : UniAction(), DumbAware {

  data class State(
    val hidden: Boolean = false,
    val decryptMe: Base64Str = Base64Str(""),
    val secretValue: String = "",
    val secretKey: String = "",
    val encryptedResult: Base64Str = Base64Str("")
  )

  sealed class Action {
    class SetDecryptMe(val str: String) : Action()
    class SetSecretValue(val str: String) : Action()
    class SetSecretKey(val str: String) : Action()
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
        is Action.DoDecrypt -> {
          s.copy(
            secretValue = s.decryptMe.aesDecrypt(s.secretKey)
          )
        }
        is Action.DoEncrypt -> {
          s.copy(
            encryptedResult = s.secretValue.aesEncryptToBase64(s.secretKey)
          )
        }
        is Action.ChangeHidden -> {
          s.copy(
            hidden = s.hidden.not()
          )
        }
      }
    }
    showPanelDialog(Uni) {
      Uni.scope.stateFlowView(this, store.stateFlow) { s ->
        row {
          label("AES")
        }
        row {
          label("secretKey:")
          myTextField(s.secretKey, s.hidden) {
            store.send(Action.SetSecretKey(it))
          }
        }
        row {
          button("decrypt") {
            store.send(Action.DoDecrypt)
          }
          label("decryptMe:")
          myTextField(s.decryptMe.str) {
            store.send(Action.SetDecryptMe(it))
          }
        }
        row {
          button("encrypt") {
            store.send(Action.DoEncrypt)
          }
          label("secret:")
          myTextArea(s.secretValue, s.hidden) {
            store.send(Action.SetSecretValue(it))
          }
        }
        row {
          label("encryptedResult:")
          myTextField("u" + "n" + "i" + "-" + "cry" + "pt" + ":" + s.encryptedResult.str) {

          }
        }
        row {
          myCheckBox("hidden",  s.hidden) {
            store.send(Action.ChangeHidden)
          }
        }
      }
    }
  }

}
