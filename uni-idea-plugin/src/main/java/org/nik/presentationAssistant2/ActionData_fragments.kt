package org.nik.presentationAssistant2

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.keymap.MacKeymapUtil
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.annotations.NotNull
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

val ActionData.fragments: ArrayList<String>
  get() {
    val fragments = ArrayList<String>()
    if (displayText.isNotEmpty()) {
      fragments.addText(displayText)
    }

    val mainKeymap = getPresentationAssistant().configuration.mainKeymap
    val shortcutTextFragments = shortcutTextFragments(mainKeymap, actionId, displayText)
    if (shortcutTextFragments.isNotEmpty()) {
      if (fragments.isNotEmpty()) fragments.addText(" via ")
      fragments.addAll(shortcutTextFragments)
    }

    val alternativeKeymap = getPresentationAssistant().configuration.alternativeKeymap
    if (alternativeKeymap != null) {
      val mainShortcut = shortcutText(mainKeymap.getKeymap()?.getShortcuts(actionId), mainKeymap.getKind())
      val altShortcutTextFragments = shortcutTextFragments(alternativeKeymap, actionId, mainShortcut)
      if (altShortcutTextFragments.isNotEmpty()) {
        fragments.addText(" (")
        fragments.addAll(altShortcutTextFragments)
        fragments.addText(")")
      }
    }
    return fragments
  }

private fun MutableList<String>.addText(text: String) {
  this.add(text)
}


private fun shortcutText(shortcuts: Array<Shortcut>?, keymapKind: KeymapKind): String =
  when {
    shortcuts == null || shortcuts.isEmpty() -> ""
    else -> shortcutText(shortcuts[0], keymapKind)
  }

private fun shortcutText(shortcut: Shortcut, keymapKind: KeymapKind): String =
  when (shortcut) {
    is KeyboardShortcut -> arrayOf(shortcut.firstKeyStroke, shortcut.secondKeyStroke).filterNotNull()
      .joinToString(separator = ", ") { shortcutText(it, keymapKind) }
    else -> ""
  }

private fun shortcutText(keystroke: KeyStroke, keymapKind: KeymapKind): String =
  when (keymapKind) {
    KeymapKind.MAC -> MacKeymapUtil.getKeyStrokeText(keystroke)
    KeymapKind.WIN -> {
      val modifiers = keystroke.modifiers
      val tokens = arrayOf(
        if (modifiers > 0) KeyEvent.getKeyModifiersText(modifiers) else null,
        getWinKeyText(keystroke.keyCode)
      )
      tokens.filterNotNull().filter { it.isNotEmpty() }.joinToString(separator = "+").trim()
    }
  }

private fun shortcutTextFragments(
  keymap: KeymapDescription,
  actionId: String,
  shownShortcut: String
): List<String> {
  val fragments = ArrayList<String>()
  val shortcutText = shortcutText(keymap.getKeymap()?.getShortcuts(actionId), keymap.getKind())
  if (shortcutText.isEmpty() || shortcutText == shownShortcut) return fragments

  when {
    keymap.getKind() == KeymapKind.WIN || SystemInfo.isMac -> {
      fragments.addText(shortcutText)
    }
    macKeyStrokesFont != null && macKeyStrokesFont!!.canDisplayUpTo(shortcutText) == -1 -> {
      fragments.add(shortcutText)
    }
    else -> {
      val altShortcutAsWin = shortcutText(keymap.getKeymap()?.getShortcuts(actionId), KeymapKind.WIN)
      if (altShortcutAsWin.isNotEmpty() && shownShortcut != altShortcutAsWin) {
        fragments.addText(altShortcutAsWin)
      }
    }
  }
  val keymapText = keymap.displayText
  if (keymapText.isNotEmpty()) {
    fragments.addText(" $keymapText")
  }
  return fragments
}

