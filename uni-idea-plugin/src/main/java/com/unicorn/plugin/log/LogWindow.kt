package com.unicorn.plugin.log

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.layout.panel
import com.unicorn.Uni
import com.unicorn.log.recent.RecentLog
import com.unicorn.plugin.ActionSubscription
import com.unicorn.plugin.showMessage
import com.unicorn.plugin.ui.showPanelDialog

class LogWindow : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          panel {
            row {
              label(Uni.buildConfig.BUILD_TIME)
            }
            row {
              button("add log") {
                Uni.log.info { "test" }
              }
            }
            row {
              button("start action subscription") {
                ActionSubscription.startSubscription()
              }
            }
            row {
              button("stop action subscription") {
                ActionSubscription.stopSubscription()
              }
            }
            row {
              button("show logs") {
                showPanelDialog {
                  RecentLog.logs.forEach { log ->
                    row {
                      textField({ log.toString() }, {})
                      button("stackTrace") {
                        showMessage(
                          log.stackTrace.map {
                            "${it.className}:${it.lineNumber}"
                          }.joinToString("\n")
                        )
                      }
                    }
                  }
                }
              }
            }
            row {
              button("clean logs") {
                RecentLog.clearLogs()
              }
            }
          },
          "",
          false
        )
      )
    }
  }
}
