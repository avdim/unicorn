package com.unicorn.plugin

import com.android.tools.idea.lang.androidSql.room.RoomUseScopeEnlarger
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.find.FindSettings
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.actions.ViewInplaceCommentsAction
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.openapi.wm.impl.status.MemoryIndicatorWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.psi.search.UseScopeEnlarger
import com.unicorn.BuildConfig
import com.unicorn.Uni
import com.unicorn.myDispose
import com.unicorn.plugin.action.Actions
import com.unicorn.plugin.ui.render.showWelcomeDialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.plugins.terminal.TerminalOptionsProvider
import java.nio.file.Path
import javax.swing.SwingConstants

private val UNICORN_KEYMAP = "Unicorn"

val dynamicPluginListener: DynamicPluginListener = object : DynamicPluginListener {
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

  val terminalLinesSize = 100_000
  try { // old IDEA 2021.1
    // Terminal settings
    val previousTerminalLines: Int = Registry.intValue("terminal.buffer.max.lines.count")
    Registry.get("terminal.buffer.max.lines.count").setValue(terminalLinesSize)
  } catch (t: Throwable) {//IDEA 2021.2
    val previousTerminalLines: Int = com.intellij.openapi.options.advanced.AdvancedSettings.getInt("terminal.buffer.max.lines.count")
    Uni.log.debug { "terminal.buffer.max.lines.count: $previousTerminalLines" }
    com.intellij.openapi.options.advanced.AdvancedSettings.setInt("terminal.buffer.max.lines.count", terminalLinesSize)
  }
  TerminalOptionsProvider.instance.setOverrideIdeShortcuts(false)//enable Alt+F2 in terminal
  TerminalOptionsProvider.instance.shellPath = "/bin/bash"

  FindSettings.getInstance().isShowResultsInSeparateView = true

  // Tab settings
  UISettings.instance.editorTabPlacement = SwingConstants.LEFT
  if (false) {
    UISettings.instance.editorTabPlacement = SwingConstants.CENTER
  }
  UISettings.instance.editorTabLimit = 40
  UISettings.instance.recentFilesLimit
  UISettings.instance.recentLocationsLimit
  UISettings.instance.state.openTabsAtTheEnd = true //option "open new tabs at the end"
  UISettings.instance.state.showCloseButton = false
  UISettings.instance.state.showFileIconInTabs = true
  UISettings.instance.state.showPinnedTabsInASeparateRow = true
  UISettings.instance.state.hideToolStripes = true
  UISettings.instance.state.showToolWindowsNumbers = true

  UISettings.instance.smoothScrolling//=false todo val //UI: smooth scrolling
  UISettings.instance.showTreeIndentGuides = true
//  UISettings.instance.showMemoryIndicator = true
  val memoryWidgetFactory = StatusBarWidgetFactory.EP_NAME.iterable.mapNotNull { it as? MemoryIndicatorWidgetFactory }.firstOrNull()
  if (memoryWidgetFactory != null) {
    //можно упростить до state.widgets[ID]
    ApplicationManager.getApplication().getService(StatusBarWidgetSettings::class.java).setEnabled(memoryWidgetFactory, true)
  }
  UISettings.instance.recentLocationsLimit// = 50 todo val
  UISettings.instance.wideScreenSupport = true
  UISettings.instance.compactTreeIndents = true

  EditorSettingsExternalizable.getInstance().isCamelWords = true //option Use "CamelHumps" words
  EditorSettingsExternalizable.getInstance().isMouseClickSelectionHonorsCamelWords = false

  EditorSettingsExternalizable.getInstance().setLineNumbersShown(false)
  EditorSettingsExternalizable.getInstance().setBreadcrumbsShown(false)
  EditorSettingsExternalizable.getInstance().isWhitespacesShown = false//todo показывает табуляцию и пробелы
  EditorSettingsExternalizable.getInstance().isWheelFontChangeEnabled = true
  if (false) {
    //todo exception in idea community 2020.2-beta
    DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = true
  }

  val keymap = KeymapManagerEx.getInstanceEx().getKeymap(UNICORN_KEYMAP)
  if (keymap != null) {//?: Uni.log.fatalError { "keymap not found $UNICORN_KEYMAP" }
    try {
      KeymapManagerEx.getInstanceEx().activeKeymap = keymap
    } catch (t: Throwable) {
      //todo fix error
      Uni.log.error { t.stackTrace }
    }
  } else {
    Uni.log.error { "keymap UNICORN_KEYMAP = $UNICORN_KEYMAP not found" }
  }

  if (true) {
    docReference<ViewInplaceCommentsAction>()
    if (false) performActionById("ViewInplaceComments")
    UISettings.instance.showInplaceComments = true
//            ViewInplaceCommentsAction.updateAllTreesCellsWidth()

//    withContext(Dispatchers.Main) {
    if (false /*TODO*/) IdeBackgroundUtil.repaintAllWindows()
//    }
  }

  GeneralSettings.getInstance().isShowTipsOnStartup = false
  GeneralSettings.getInstance().isUseSafeWrite = true
  if (true) {
    // когда перестанет вылетать вернуть на default
    GeneralSettings.getInstance().isAutoSaveIfInactive = true
    GeneralSettings.getInstance().inactiveTimeout = 5
  }
  GeneralSettings.getInstance().isReopenLastProject = false

//        ServiceManager.getService(StatusBarWidgetSettings::class.java).setEnabled()

  val laf = LafManager.getInstance()
  val comboBoxModel = laf.lafComboBoxModel
  val ids = laf.lafComboBoxModel.items.map { it.themeId }
  Uni.log.debug { "theme ids: $ids" }
  val preferredTheme = comboBoxModel.items.firstOrNull { it.themeId == "JetBrainsLightTheme" }
  val newLaf = laf.findLaf(preferredTheme)
  if (laf.currentLookAndFeel != newLaf) {
    QuickChangeLookAndFeel.switchLafAndUpdateUI(laf, newLaf, true)
  }

  UISettings.instance.fireUISettingsChanged()
  EditorFactory.getInstance().refreshAllEditors()

  UseScopeEnlarger.EP_NAME.point.unregisterExtension(RoomUseScopeEnlarger::class.java)//todo иначе падает при rename in fileManager

  MainScope().launch {
    if (BuildConfig.HAND_TEST) {
      invokeLater {
        com.intellij.ide.impl.ProjectUtil.openOrImport(Path.of(BuildConfig.HAND_TEST_EMPTY_PROJECT))
      }
    } else {
      showWelcomeDialog()
    }
  }
  Uni.log.debug { "configureIDE end" }
}
