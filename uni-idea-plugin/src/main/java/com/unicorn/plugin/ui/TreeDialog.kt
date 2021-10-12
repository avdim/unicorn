package com.unicorn.plugin.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.containers.Convertor
import com.unicorn.Uni
import com.unicorn.plugin.TreeAction
import com.unicorn.plugin.TreeState
import com.unicorn.plugin.ui.render.stateFlowView
import ru.avdim.mvi.createStore
import java.util.*
import javax.swing.JTree
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun showTreeDialog() {
  val store = createStore(TreeState(0)) { s, a: TreeAction -> s.copy(s.i + 1) }
  showPanelDialog(Uni) {
    Uni.scope.stateFlowView(this, store.stateFlow) {
      row {
        val list = JBList<String>()
//        list.setDataProvider(DataProvider { listOf("a", "b") })
        list.setListData(
          arrayOf("a", "b")
        )
        list()
      }
      row {
        label("Tree view")
        val tree = Tree(
          ListTreeNode(
            "root",
            Leaf("aaa"),
            ListTreeNode(
              "2",
              Leaf("2aaa"),
              Leaf("2bbb"),
            ),
            Leaf("bbb"),
          )
        )
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
            setIcon(com.android.tools.idea.ui.wizard.DIR_ICON)
          }

        })
        TreeSpeedSearch(tree) { it?.lastPathComponent?.toString() ?: "empty" }
        tree()
      }
    }
  }
}

fun Leaf(content: String): TreeNode {
  return object : TreeNode {
    override fun getChildAt(childIndex: Int): TreeNode = TODO()
    override fun getChildCount(): Int = 0
    override fun getParent(): TreeNode = this
    override fun getIndex(node: TreeNode?): Int = -1
    override fun getAllowsChildren(): Boolean = false
    override fun isLeaf(): Boolean = true
    override fun children(): Enumeration<out TreeNode> =
      Collections.enumeration(emptyList())

    override fun toString(): String = content
  }
}

fun ListTreeNode(text: String, vararg items: TreeNode): TreeNode {
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
    override fun children(): Enumeration<out TreeNode> =
      Collections.enumeration(childs)

    override fun toString(): String = text
  }
}
