// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import java.util.Comparator

class GroupByTypeComparator2 : Comparator<AbstractTreeNod2<*>?> {
  private val _isAbbreviateQualifiedNames: Boolean = false
  private val _isSortByType: Boolean = false
  private val _isManualOrder: Boolean = false
  private val _isFoldersAlwaysOnTop: Boolean = false

  override fun compare(descriptor1: AbstractTreeNod2<*>?, descriptor2: AbstractTreeNod2<*>?): Int {
    var descriptor1 = descriptor1
    var descriptor2 = descriptor2
    descriptor1 = getNodeDescriptor(descriptor1)
    descriptor2 = getNodeDescriptor(descriptor2)
    if (descriptor1 is ProjectViewNode<*> && descriptor2 is ProjectViewNode<*>) {
      val node1 = descriptor1
      val node2 = descriptor2
      if (_isManualOrder) {
        val key1 = node1.manualOrderKey
        val key2 = node2.manualOrderKey
        val result = CompareUtil.compare(key1, key2)
        if (result != 0) return result
      }
      if (_isFoldersAlwaysOnTop) {
        val typeWeight1 = node1.getTypeSortWeight(_isSortByType)
        val typeWeight2 = node2.getTypeSortWeight(_isSortByType)
        if (typeWeight1 != 0 && typeWeight2 == 0) {
          return -1
        }
        if (typeWeight1 == 0 && typeWeight2 != 0) {
          return 1
        }
        if (typeWeight1 != 0 && typeWeight2 != typeWeight1) {
          return typeWeight1 - typeWeight2
        }
      }
      if (_isSortByType) {
        val typeSortKey1 = node1.typeSortKey
        val typeSortKey2 = node2.typeSortKey
        val result = CompareUtil.compare(typeSortKey1, typeSortKey2)
        if (result != 0) return result
      } else {
        val typeSortKey1 = node1.sortKey
        val typeSortKey2 = node2.sortKey
        if (typeSortKey1 != null && typeSortKey2 != null) {
          val result = CompareUtil.compare(typeSortKey1, typeSortKey2)
          if (result != 0) return result
        }
      }
      if (_isAbbreviateQualifiedNames) {
        val key1 = node1.qualifiedNameSortKey
        val key2 = node2.qualifiedNameSortKey
        if (key1 != null && key2 != null) {
          return StringUtil.naturalCompare(key1, key2)
        }
      }
    }
    return if (descriptor1 == null) {
      -1
    } else {
      if (descriptor2 == null) {
        1
      } else {
        AlphaComparator2.compare(descriptor1, descriptor2)
      }
    }
  }

  private fun getNodeDescriptor(descriptor: AbstractTreeNod2<*>?): AbstractTreeNod2<*>? {
    var current = descriptor
//    if (!_isSortByType && current is ProjectViewNode<*> && current.isSortByFirstChild) {
//      val children = current.children
//      if (!children.isEmpty()) {
//        current = children.iterator().next()
//        current!!.update()
//      }
//    }
    return current
  }

}
