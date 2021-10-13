package com.unicorn.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.unicorn.Uni
import java.util.*
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

fun showTreeDialog() {
  showPanelDialog(Uni) {

    val mutableListTreeNode = MutableListTreeNode(
      "root",
      Leaf("aaa"),
      MutableListTreeNode(
        "2",
        Leaf("2aaa"),
        Leaf("2bbb"),
      ),
      Leaf("bbb"),
    )
    val defaultTreeModel: DefaultTreeModel = DefaultTreeModel(mutableListTreeNode)

    row {
      button("add new") {
        defaultTreeModel.insertNodeInto(
          Leaf("new"),
          mutableListTreeNode,
          0
        )
        mutableListTreeNode.remove(0)
      }
    }

    row {
      label("Tree view")
      val tree = Tree(defaultTreeModel)
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
      tree()
      tree.addTreeWillExpandListener(object : TreeWillExpandListener {
        override fun treeWillExpand(event: TreeExpansionEvent) {
          val expandedNode = event.path.lastPathComponent as MutableTreeNode
          defaultTreeModel.insertNodeInto(
            Leaf("will expand"),
            expandedNode,
            0
          )
        }

        override fun treeWillCollapse(event: TreeExpansionEvent) {

        }
      })
    }
  }
}

private fun Leaf(content: String): MutableTreeNode {
  return DefaultMutableTreeNode(content, false)
}

private fun MutableListTreeNode(text: String, vararg items: MutableTreeNode): MutableTreeNode {
  val node = DefaultMutableTreeNode(text, true)
  items.forEach {
    node.add(it)
  }
  return node
}

private fun ListTreeNode(text: String, vararg items: TreeNode): TreeNode {
  return object : TreeNode {
    val listParent = this
    val childs: List<TreeNode> = items.map {
      object : TreeNode by it {
        override fun getParent(): TreeNode = listParent
        override fun toString(): String = it.toString()
      }
    }

    override fun getChildAt(childIndex: Int): TreeNode = childs[childIndex]
    override fun getChildCount(): Int = childs.size
    override fun getParent(): TreeNode = this
    override fun getIndex(node: TreeNode?): Int = node?.let { childs.indexOf(it) } ?: -1
    override fun getAllowsChildren(): Boolean = true
    override fun isLeaf(): Boolean = false
    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(childs)

    override fun toString(): String = text
  }
}
