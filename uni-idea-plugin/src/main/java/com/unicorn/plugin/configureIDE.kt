package com.unicorn.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ViewInplaceCommentsAction
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.unicorn.Uni
import org.jetbrains.plugins.terminal.TerminalOptionsProvider
import javax.swing.SwingConstants

private val UNICORN_KEYMAP = "Unicorn"

suspend fun configureIDE() {
  Uni.log.info { "configureIDE" }
  // Upload plugin timeout
  Registry.get("ide.plugins.unload.timeout").setValue(15_000)
  // Terminal settings
  val previousTerminalLines: Int = Registry.intValue("terminal.buffer.max.lines.count")
  Registry.get("terminal.buffer.max.lines.count").setValue(100_000)
  TerminalOptionsProvider.instance.setOverrideIdeShortcuts(false)//enable Alt+F2 in terminal
//  TerminalOptionsProvider.instance.setShellPath("/bin/bash")//todo work's on intellij {  version = "202.5103.13-EAP-SNAPSHOT"

  // Tab settings
  UISettings.instance.editorTabPlacement = SwingConstants.LEFT
  UISettings.instance.editorTabPlacement = SwingConstants.CENTER
  UISettings.instance.editorTabLimit = 1
  UISettings.instance.recentFilesLimit
  UISettings.instance.recentLocationsLimit
  UISettings.instance.state.openTabsAtTheEnd = true //option "open new tabs at the end"
  EditorSettingsExternalizable.getInstance().isCamelWords = true //option Use "CamelHumps" words
  EditorSettingsExternalizable.getInstance().isMouseClickSelectionHonorsCamelWords = false

  EditorSettingsExternalizable.getInstance().setLineNumbersShown(false)
  EditorSettingsExternalizable.getInstance().setBreadcrumbsShown(false)
  EditorSettingsExternalizable.getInstance().isWhitespacesShown = false//todo показывает табуляцию и пробелы
  if (false) {
    //todo exception in idea community 2020.2-beta
    DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = true
  }

  KeymapManagerEx.getInstanceEx().activeKeymap =
    KeymapManagerEx.getInstanceEx().getKeymap(UNICORN_KEYMAP) ?: Uni.log.fatalError { "keymap not found $UNICORN_KEYMAP" }

  UISettings.instance.smoothScrolling//=false todo val //UI: smooth scrolling
  UISettings.instance.showTreeIndentGuides = true
  UISettings.instance.showMemoryIndicator = true
  UISettings.instance.recentLocationsLimit// = 50 todo val
  UISettings.instance.wideScreenSupport = false

  if (true) {
    docReference<ViewInplaceCommentsAction>()
    if (false) performActionById("ViewInplaceComments")
    UISettings.instance.showInplaceComments = true
//            ViewInplaceCommentsAction.updateAllTreesCellsWidth()

//    withContext(Dispatchers.Main) {
      if(false /*TODO*/) IdeBackgroundUtil.repaintAllWindows()
//    }
  }

  GeneralSettings.getInstance().isUseSafeWrite = true
  if (true) {
    // когда перестанет вылетать вернуть на default
    GeneralSettings.getInstance().isAutoSaveIfInactive = true
    GeneralSettings.getInstance().inactiveTimeout = 5
  }
  GeneralSettings.getInstance().isReopenLastProject = false

//        ServiceManager.getService(StatusBarWidgetSettings::class.java).setEnabled()

  UISettings.instance.fireUISettingsChanged()
  EditorFactory.getInstance().refreshAllEditors()
}
