package com.unicorn.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ViewInplaceCommentsAction
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.unicorn.plugin.action.Actions
import com.unicorn.Uni
import com.unicorn.myDispose
import com.unicorn.plugin.action.id.openDialogFileManager
import kotlinx.coroutines.*
import org.jetbrains.plugins.terminal.TerminalOptionsProvider
import javax.swing.SwingConstants

private val UNICORN_KEYMAP = "Unicorn"

val dynamicPluginListener: DynamicPluginListener = object : DynamicPluginListener{
  override fun beforePluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.beforePluginLoaded 1" }
  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.pluginLoaded 2" }
  }

  override fun checkUnloadPlugin(pluginDescriptor: IdeaPluginDescriptor) {
    Uni.log.debug { "UniDynamicListener.checkUnloadPlugin 1" }
  }

  override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    Uni.log.debug { "UniDynamicListener.beforePluginUnload 2" }
    Uni.log.debug { "pluginDescriptor.name: ${pluginDescriptor.name}" }
    if (pluginDescriptor.name == Uni.PLUGIN_NAME) {
      Uni.myDispose()
      //todo dynamic plugin can't unload when tool window is open
    }
  }

  override fun pluginUnloaded(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    Uni.log.debug { "UniDynamicListener.pluginUnloaded start 3" }
    Uni.log.debug { "UniDynamicListener.pluginUnloaded finish 3" }
  }
}

suspend fun configureIDE() {
  Uni.log.debug { "configureIDE begin" }
  ApplicationManager.getApplication().messageBus.connect(Uni)
  .subscribe(DynamicPluginListener.TOPIC, dynamicPluginListener)

  Actions.register()
  // Upload plugin timeout
  Registry.get("ide.plugins.unload.timeout").setValue(8_000)
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

  val keymap = KeymapManagerEx.getInstanceEx().getKeymap(UNICORN_KEYMAP)
  if (keymap != null) {//?: Uni.log.fatalError { "keymap not found $UNICORN_KEYMAP" }
    KeymapManagerEx.getInstanceEx().activeKeymap = keymap
  } else {
    Uni.log.error { "keymap $UNICORN_KEYMAP not found" }
  }

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

  if (Uni.buildConfig.OPEN_FILE_MANAGER_AT_START) {
    delay(500)
    openDialogFileManager()
  }

  Uni.log.debug { "configureIDE end" }
}
