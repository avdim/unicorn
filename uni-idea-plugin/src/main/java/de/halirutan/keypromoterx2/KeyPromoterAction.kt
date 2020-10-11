/*
 * Copyright (c) 2017 Patrick Scheibe, Dmitry Kashin, Athiele.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.halirutan.keypromoterx2

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionMenuItem
import com.intellij.openapi.actionSystem.impl.actionholder.ActionRef
import com.intellij.openapi.wm.impl.StripeButton
import org.apache.commons.lang.StringUtils
import java.awt.AWTEvent
import java.lang.reflect.Field
import java.util.*
import javax.swing.JButton

/**
 * Provides a way to extract the idea action from an AWT event. This is the class where the magic happens. We try hard
 * to extract the IDEA action that was invoked from an AWT event. On our way, we need to extract private fields of
 * IDEA classes and still, there are cases when we won't be able to extract the action that was invoked.
 *
 * @author patrick (22.06.17).
 */
class KeyPromoterAction {
  var source = ActionSource.INVALID
  private var myMnemonic = 0
  private var myShortcut = ""
  var description: String = ""
  var ideaActionID: String = ""

  /**
   * Constructor used when have to fall back to inspect an AWT event instead of actions that are directly provided
   * by IDEA. Tool-window stripe buttons are such a case where I'm not notified by IDEA if one is pressed
   *
   * @param event mouse event that happened
   */
  constructor(event: AWTEvent) {
    when (val source = event.source) {
      is ActionButton -> analyzeActionButton(source)
      is StripeButton -> analyzeStripeButton(source)
      is ActionMenuItem -> analyzeActionMenuItem(source)
      is JButton -> analyzeJButton(source)
    }
  }

  /**
   * Constructor used when we get notified by IDEA through [com.intellij.openapi.actionSystem.ex.AnActionListener]
   *
   * @param action action that was performed
   * @param event  event that fired the action
   * @param source the source of the action
   */
  internal constructor(action: AnAction?, event: AnActionEvent, source: ActionSource) {
    if(action != null) {
      ideaActionID = ActionManager.getInstance().getId(action)
      description = event.presentation.text
      this.source = source
      myShortcut = KeyPromoterUtils.getKeyboardShortcutsText(ideaActionID)
      fixDescription()
    }
  }

  /**
   * Information extraction for buttons on the toolbar
   *
   * @param source source of the action
   */
  private fun analyzeActionButton(source: ActionButton) {
    val action = source.action
    action?.let { fixValuesFromAction(it) }
    this.source = ActionSource.MAIN_TOOLBAR
  }

  /**
   * Information extraction for entries in the menu
   *
   * @param source source of the action
   */
  private fun analyzeActionMenuItem(source: ActionMenuItem) {
    this.source = ActionSource.MENU_ENTRY
    description = source.text
    myMnemonic = source.mnemonic
    val actionField = findActionField(source, ActionRef::class.java)
    if (actionField != null) {
      try {
        val o = actionField[source] as ActionRef<*>
        val action = o.action
        action?.let { fixValuesFromAction(it) }
      } catch (e: Exception) {
        // happens..
      }
    }
  }

  /**
   * Information extraction for buttons of tool-windows
   *
   * @param source source of the action
   */
  private fun analyzeStripeButton(source: StripeButton) {
    this.source = ActionSource.TOOL_WINDOW_BUTTON
    description = source.text
    myMnemonic = source.mnemonic2
    if (myMnemonic > 0) {
      description = description.replaceFirst("\\d: ".toRegex(), "")
    }
    // This is hack, but for IDEA stripe buttons it doesn't seem possible to extract the IdeaActionID.
    // We turn e.g. "9: Version Control" to "ActivateVersionControlToolWindow" which seems to work for all tool windows
    // in a similar way.
    ideaActionID = KeyPromoterBundle.message(
      "kp.stripe.actionID",
      StringUtils.replace(description, " ", "")
    )
    myShortcut = KeyPromoterUtils.getKeyboardShortcutsText(ideaActionID)
  }

  /**
   * Information extraction for all other buttons
   * TODO: This needs to be tested. I couldn't find a button that wasn't inspected with this fallback.
   *
   * @param source source of the action
   */
  private fun analyzeJButton(source: JButton) {
    this.source = ActionSource.OTHER
    myMnemonic = source.mnemonic
    description = source.text
  }

  /**
   * Extracts a private field from a class so that we can access it for getting information
   *
   * @param source Object that contains the field we are interested in
   * @param target Class of the field we try to extract
   *
   * @return The field that was found
   */
  private fun findActionField(source: Any, target: Class<*>): Field? {
    val field: Field?
    if (!myClassFields.containsKey(source.javaClass)) {
      field = KeyPromoterUtils.getFieldOfType(source.javaClass, target)
      if (field == null) {
        return null
      }
      myClassFields[source.javaClass] = field
    } else {
      field = myClassFields[source.javaClass]
    }
    return field
  }

  /**
   * This method can be used at several places to update shortcut, description and ideaAction from an [AnAction]
   *
   * @param anAction action to extract values from
   */
  private fun fixValuesFromAction(anAction: AnAction) {
    description = anAction.templatePresentation.text
    ideaActionID = ActionManager.getInstance().getId(anAction)
    myShortcut = KeyPromoterUtils.getKeyboardShortcutsText(ideaActionID)
  }

  /**
   * Used to adjust Run and Debug descriptions so that the don't contain the name of the run-configuration
   */
  private fun fixDescription() {
    if (description.isEmpty()) {
      return
    }
    if ("Debug" == ideaActionID) {
      description = description.replaceFirst("Debug '.*'".toRegex(), "Debug")
    }
    if ("Run" == ideaActionID) {
      description = description.replaceFirst("Run '.*'".toRegex(), "Run")
    }
  }

  val shortcut: String
    get() {
      if (myShortcut.isNotEmpty()) {
        return myShortcut
      }
      if (source == ActionSource.TOOL_WINDOW_BUTTON && myMnemonic > 0) {
        myShortcut = "\'" + metaKey + myMnemonic.toChar() + "\'"
      }
      return myShortcut
    }

  /**
   * Checks if we have all necessary information about an action that was invoked
   *
   * @return true if it has a description, an actionID and a shortcut
   */
  val isValid: Boolean get() = description != "" && ideaActionID != ""

  enum class ActionSource {
    MAIN_TOOLBAR, TOOL_WINDOW_BUTTON, MENU_ENTRY, POPUP, OTHER, INVALID
  }

  companion object {
    private val metaKey = if (System.getProperty("os.name")
        .contains("OS X")
    ) KeyPromoterBundle.message("kp.meta.osx") else KeyPromoterBundle.message("kp.meta.default")

    // Fields with actions of supported classes
    private val myClassFields: MutableMap<Class<*>, Field> = HashMap(5)
  }
}