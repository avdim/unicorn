package com.unicorn.plugin.ui.render.todo

import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.ui.FileNode
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.layout.Row
import com.intellij.ui.tree.project.ProjectFileTreeModel
import com.intellij.util.ui.tree.AbstractFileTreeTable
import org.jdesktop.swingx.treetable.FileSystemModel
import org.jdesktop.swingx.treetable.SimpleFileSystemModel
import javax.swing.tree.DefaultTreeModel

fun Row.simpleFilesTree(dir: VirtualFile, project: Project) {
  val node1 = AbstractFileTreeTable.FileNode(dir, project)
  val node2 = CheckedTreeNode()
  val node3 = FileNode(dir, project, true)
  val model1 = DefaultTreeModel(node1)
  val model2 = FileSystemModel()
  val model3 = SimpleFileSystemModel()
  val model4 = ProjectFileTreeModel(project)
  val component: ProjectViewTree =
      ProjectViewTree(model1)
  component.addTreeSelectionListener { it.path }
  component()
}