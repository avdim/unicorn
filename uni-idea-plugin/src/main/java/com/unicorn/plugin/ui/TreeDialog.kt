package com.unicorn.plugin.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import com.unicorn.Uni
import com.unicorn.plugin.TreeAction
import com.unicorn.plugin.TreeState
import com.unicorn.plugin.ui.render.stateFlowView
import ru.avdim.mvi.createStore
import java.util.*
import javax.swing.JTree
import javax.swing.tree.TreeNode

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
        val tree = Tree(object: TreeNode {
          override fun getChildAt(childIndex: Int): TreeNode = TODO()
          override fun getChildCount(): Int = 0
          override fun getParent(): TreeNode = this
          override fun getIndex(node: TreeNode?): Int = -1
          override fun getAllowsChildren(): Boolean = false
          override fun isLeaf(): Boolean = true
          override fun children(): Enumeration<out TreeNode> =
            Collections.enumeration(emptyList())
          override fun toString(): String = "I am root"
        })
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
        tree()
      }
    }
  }
}
