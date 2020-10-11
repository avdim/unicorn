package com.unicorn.plugin.toast

import com.intellij.openapi.project.Project
import java.util.*

object Toast {
  private var infoPanel: ToastPanel? = null

  fun show(project: Project, textFragments: ArrayList<String>) {
    val infoPanel = infoPanel
    if (infoPanel == null || !infoPanel.canBeReused()) {
      Toast.infoPanel = ToastPanel(project, textFragments)
    } else {
      infoPanel.updateText(project, textFragments)
    }
  }
}
