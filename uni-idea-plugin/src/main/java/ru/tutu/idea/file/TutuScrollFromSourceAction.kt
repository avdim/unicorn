// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

//internal class TutuScrollFromSourceAction(private val myView: TutuProjectViewImpl) : AnAction(
//  //todo Сделать action который найдёт открывый файл в файловом менеджере
//
//  IdeBundle.lazyMessage("action.AnAction.text.select.opened.file"),
//  IdeBundle.lazyMessage("action.AnAction.description.select.opened.file"),
//  AllIcons.General.Locate), DumbAware {
//  override fun actionPerformed(e: AnActionEvent) {
//
//  }
//  override fun update(event: AnActionEvent) {
//    val presentation = event.presentation
//    presentation.text = "Select Opened File" + myView.scrollToSourceShortcut
//    presentation.isEnabledAndVisible = !myView.isAutoscrollFromSource
//  }
//
//}
