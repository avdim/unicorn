package com.intellij.my.file

import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.ide.util.treeView.FileNameComparator

object AlphaComparator2 {
    fun compare(a: AbstractTreeNod2<*>, b: AbstractTreeNod2<*>): Int {
        val weight1 = a.getWeight()
        val weight2 = b.getWeight()
        if (weight1 != weight2) {
            return weight1 - weight2
        }
        val s1 = a.sortedName
        val s2 = b.sortedName
        return FileNameComparator.INSTANCE.compare(s1, s2)
    }
}
