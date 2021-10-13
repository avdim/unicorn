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
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.*
import kotlin.concurrent.thread
import kotlin.streams.toList

private sealed class Action {
  object Increment : Action()
}

private data class State(
  val i: Int
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

fun useDir(path: Path) = DefaultMutableTreeNode(path, true).also {
  it.add(DefaultMutableTreeNode(it.userObject, false))//todo stub
}

fun useFile(path: Path) = DefaultMutableTreeNode(path, false)

private fun DefaultMutableTreeNode.expand(treeModel: DefaultTreeModel) {
  val path = userObject as Path

  removeAllChildren()
  val pathToNode = Files.list(path)//todo may be exception
    .toList().associateWith {
      useDirOrFile(it)
    }.toConcurrentHashMap()
  pathToNode.values.forEach {
    treeModel.insertNodeInto(it, this@expand, 0)
  }
  path.registerListener { e: PathListenerEvent ->
    when (e.type) {
      PathListenerEvent.Type.New -> {
        pathToNode[e.path] = useDirOrFile(e.path).also {
          treeModel.insertNodeInto(it, this@expand, 0)
        }
      }
      PathListenerEvent.Type.Delete -> {
        val node = pathToNode[e.path]!!
        treeModel.removeNodeFromParent(node)
      }
    }
  }
}

private fun DefaultMutableTreeNode.collapse() {
  //todo cancel subscription
}

private fun useDirOrFile(it: Path) = if (it.isDirectory()) {
  useDir(it)
} else {
  useFile(it)
}

fun Path.registerListener(listener: (PathListenerEvent) -> Unit) {
  val dir = this
  thread {//todo BAD thread
    val ws = dir.fileSystem.newWatchService()
    dir.register(
      ws,
      StandardWatchEventKinds.OVERFLOW,//todo
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
//      StandardWatchEventKinds.ENTRY_MODIFY
    )

    while (true) {
      val key: WatchKey = try {
        ws.take()
      } catch (t: Throwable) {
        println("catch")
        continue
      }
      try {
        val events = key.pollEvents()
        events.forEach {
          val kind = it.kind()
          val context = it.context()
          val p = (context as? Path)?.let {
            dir.resolve(it)
          }
          println("kind: $kind, context: $context")
          when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
              if (p != null) {
                listener(PathListenerEvent(PathListenerEvent.Type.New, p))
              }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
              if (p != null) {
                listener(PathListenerEvent(PathListenerEvent.Type.Delete, p))
              }
            }
          }
        }
      } finally {
        key.reset()
      }
    }
  }
}

val TreeExpansionEvent.lastNode
  get():DefaultMutableTreeNode =
    path.lastPathComponent as DefaultMutableTreeNode

class PathListenerEvent(val type: Type, val path: Path) {
  sealed class Type {
    object New : Type()
    object Delete : Type()
  }
}

fun <K, V> Map<K, V>.toConcurrentHashMap(): MutableMap<K, V> {
  val result = ConcurrentHashMap<K, V>()
  result.putAll(this)
  return result
}
