package com.unicorn.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.io.isDirectory
import com.unicorn.Uni
import com.unicorn.plugin.ui.render.stateFlowView
import ru.avdim.mvi.createStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.*

private sealed class Action{
  object Increment:Action()
}
private data class State(
  val i:Int
)

fun showFileTreeDialog() {
  val store = createStore(State(0)) { s, a: Action -> s.copy(s.i + 1) }
  showPanelDialog(Uni) {
    Uni.scope.stateFlowView(this, store.stateFlow) { s: State ->

      val root = DefaultMutableTreeNode(Paths.get("/"), true)
      val defaultTreeModel: DefaultTreeModel = DefaultTreeModel(root)
      root.expand(defaultTreeModel)

      row {
        button("update mvi") {
          store.send(Action.Increment)
        }
        label("counter: ${s.i}")
      }
      row {
        val tree = Tree(defaultTreeModel)
        tree.autoscrolls = true
        tree.setCellRenderer(object : ColoredTreeCellRenderer() {
          override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
          ) {
            append(value.toString(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
            if (leaf) {
              //AllIcons.Nodes.Symlink
              setIcon(AllIcons.FileTypes.Text)
            } else {
              setIcon(AllIcons.Nodes.Folder)
            }
          }
        })
        TreeSpeedSearch(tree) { it?.lastPathComponent?.toString() ?: "empty" }
        scrollPane(tree)

        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
          override fun treeWillExpand(event: TreeExpansionEvent) {
            val expandedNode = event.lastNode
            val path = expandedNode.userObject as Path
            expandedNode.expand(defaultTreeModel)
          }
          override fun treeWillCollapse(event: TreeExpansionEvent) {
            val collapsedNode = event.lastNode
            val path = collapsedNode.userObject as Path
            collapsedNode.collapse()
          }
        })
      }
    }
  }
}

fun useDir(path:Path) = DefaultMutableTreeNode(path, true).also {
  it.add(DefaultMutableTreeNode(it.userObject, false))//todo stub
}
fun useFile(path:Path) = DefaultMutableTreeNode(path, false)

private fun DefaultMutableTreeNode.expand(treeModel: DefaultTreeModel) {
  val path = userObject as Path

  removeAllChildren()
  Files.list(path).forEach { //todo may be exception
    val child = if (it.isDirectory()) {
      useDir(it)
    } else {
      useFile(it)
    }
    treeModel.insertNodeInto(child, this@expand, 0)
  }
}

private fun DefaultMutableTreeNode.collapse() {
  //todo cancel subscription
}

val TreeExpansionEvent.lastNode get():DefaultMutableTreeNode =
  path.lastPathComponent as DefaultMutableTreeNode

