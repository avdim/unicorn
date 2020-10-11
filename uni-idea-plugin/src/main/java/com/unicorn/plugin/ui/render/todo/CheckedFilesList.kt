package com.unicorn.plugin.ui.render.todo

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesTreeImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.Row
import com.unicorn.Uni
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener

fun Row.checkedFilesList(
    project: Project,
    files: List<VirtualFile>
) {
  val filesView = ChangesTreeImpl.VirtualFiles(
      project, true, true,
      files
  )
  filesView.addTreeSelectionListener {
    Uni.log.debug { "addTreeSelectionListener" }
    val selectedChanges = filesView.selectedChanges
    Uni.log.debug { "selectedChanges: $selectedChanges" }
  }
  filesView.addTreeExpansionListener(object : TreeExpansionListener {
    override fun treeExpanded(p0: TreeExpansionEvent?) {
      Uni.log.debug { "addTreeExpansionListener treeExpanded" }
    }

    override fun treeCollapsed(p0: TreeExpansionEvent?) {
      Uni.log.debug { "addTreeExpansionListener treeCollapsed" }
    }
  })
  filesView()
}