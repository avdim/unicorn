package com.unicorn.plugin.action.terminal

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.unicorn.plugin.getToolWindow
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory

val Project.terminalToolWindow get() = getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

