@file:Suppress("UnstableApiUsage")

package com.intellij.ide.projectView.impl

import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

abstract class AbstractProjectViewPaneMiddle(project: Project) : AbstractProjectViewPane2(project) {
  @JvmField
  var myAsyncSupport: AsyncProjectViewSupport? = null

  override fun installComparator(builder: AbstractTreeBuilder, comparator: Comparator<in NodeDescriptor<*>>) {
    myAsyncSupport?.setComparator(comparator)
    super.installComparator(builder, comparator)
  }

}
