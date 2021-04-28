package com.unicorn.plugin.ui.render

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.LayoutBuilder
import com.unicorn.plugin.performActionById
import com.intellij.my.file.uniFiles
import com.intellij.ui.layout.CellBuilder
import com.unicorn.Uni
import com.unicorn.plugin.mvi.Column
import com.unicorn.plugin.mvi.UniWindowState
import todo.mvi.Intent
import java.io.File
import javax.swing.JComponent

val TODO_TEXT_FIELD_COMPLETION = true//todo

fun fileManager(
  layoutBuilder: LayoutBuilder,
  state: UniWindowState,
  project: Project,
  send: (Intent) -> Unit
) {
  val dir: VirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath("/")!!
  val files = dir.children.toList()
  layoutBuilder.row {
    cell(isVerticalFlow = false) {
      button("universal action") {
        //todo register action programmatically
        performActionById("TutuPlugin.ActionUniversal")
      }
      button("add column") {
        send(Intent.AddColumn())
      }
    }
  }
  layoutBuilder.row {
    state.columns.forEachIndexed { col: Int, column: Column ->
      cell(isVerticalFlow = true) {
        button("remove column") {
          send(Intent.RemoveColumn(col))
        }
        column.paths.forEachIndexed { row: Int, path: String ->
          if (TODO_TEXT_FIELD_COMPLETION) textFieldCompletion(
            project = Uni.todoDefaultProject,
            label = null,
            currentValue = path,
            autoCompletionVariants = File(path).listFiles()?.toList().orEmpty().map { it.path }
          ) {
            if (it.isEmpty()) {
              send(Intent.RemovePath(col, row))
            } else {
              send(Intent.EditPath(col, row, it))
            }
          }
        }
        button("+") {
          send(Intent.AddPath(col))
        }
        if (state.renderFiles) {
          uniFiles(project, rootPaths = column.paths)
        }
      }
    }
  }
  layoutBuilder.row {
    if (state.renderFiles) {
      button("update") {
        send(Intent.Update)
      }
    } else {
      button("render files") {
        send(Intent.RenderFiles)
      }
    }
  }
//        row { checkedFilesList(project, files) }
//        row { checkedFilesList2(project, files) }
//        row { simpleFilesTree(dir, project) }
}
