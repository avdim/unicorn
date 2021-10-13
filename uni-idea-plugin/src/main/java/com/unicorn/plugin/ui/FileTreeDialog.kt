package com.unicorn.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.io.isDirectory
import com.unicorn.Uni
import com.unicorn.file.PathListenerEvent
import com.unicorn.file.addListener
import com.unicorn.plugin.ui.render.stateFlowView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.supervisorScope
import ru.avdim.mvi.createStore
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.*
import kotlin.streams.toList

private sealed class Action {
  object Increment : Action()
}

private data class State(
  val i: Int
)

fun showFileTreeDialog() {
  val store = createStore(State(0)) { s, a: Action -> s.copy(s.i + 1) }
  showPanelDialog(Uni) { dialogScope: CoroutineScope ->
    Uni.scope.stateFlowView(this, store.stateFlow) { s: State ->

      val root = DefaultMutableTreeNode(Paths.get("/"), true)
      val defaultTreeModel: DefaultTreeModel = DefaultTreeModel(root)
      root.expand(dialogScope, defaultTreeModel)

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

        val pathToJob:MutableMap<Path, Job> = ConcurrentHashMap()

        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
          override fun treeWillExpand(event: TreeExpansionEvent) {
            val expandedNode = event.lastNode
            val path = expandedNode.userObject as Path
            val job = expandedNode.expand(dialogScope, defaultTreeModel)
            pathToJob[path] = job
          }

          override fun treeWillCollapse(event: TreeExpansionEvent) {
            val collapsedNode = event.lastNode
            val path = collapsedNode.userObject as Path
            collapsedNode.collapse(
              pathToJob.remove(path)!!
            )
          }
        })
      }
    }
  }
}

fun useDir(path: Path) = DefaultMutableTreeNode(path, true).also {
  it.add(DefaultMutableTreeNode(it.userObject, false))//todo stub
}

fun useFile(path: Path) = DefaultMutableTreeNode(path, false)

private fun DefaultMutableTreeNode.expand(scope:CoroutineScope, treeModel: DefaultTreeModel): Job {
  val path = userObject as Path

  this.children().toList().forEach {
    treeModel.removeNodeFromParent(it as MutableTreeNode)
  }
  val pathToNode = Files.list(path)//todo may be exception
    .toList().associateWith {
      useDirOrFile(it)
    }.toConcurrentHashMap()
  pathToNode.values.forEach {
    treeModel.insertNodeInto(it, this@expand, 0)
  }
  val job = path.addListener(scope) { e: PathListenerEvent ->
    when (e.type) {
      PathListenerEvent.Type.New -> {
        pathToNode[e.path] = useDirOrFile(e.path).also {
          treeModel.insertNodeInto(it, this@expand, 0)
        }
      }
      PathListenerEvent.Type.Delete -> {
        val node = pathToNode.remove(e.path)!!
        treeModel.removeNodeFromParent(node)
      }
    }
  }
  return job
}

private fun DefaultMutableTreeNode.collapse(job:Job) {
  job.cancel()
}

private fun useDirOrFile(it: Path) = if (it.isDirectory()) {
  useDir(it)
} else {
  useFile(it)
}

private val TreeExpansionEvent.lastNode
  get():DefaultMutableTreeNode =
    path.lastPathComponent as DefaultMutableTreeNode

fun <K, V> Map<K, V>.toConcurrentHashMap(): MutableMap<K, V> {
  val result = ConcurrentHashMap<K, V>()
  result.putAll(this)
  return result
}
