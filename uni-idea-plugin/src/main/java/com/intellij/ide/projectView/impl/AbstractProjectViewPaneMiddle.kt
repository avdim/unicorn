@file:Suppress("UnstableApiUsage")

package com.intellij.ide.projectView.impl

import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

abstract class AbstractProjectViewPaneMiddle(project: Project) : AbstractProjectViewPane2(project) {

  override fun installComparator(builder: AbstractTreeBuilder, comparator: Comparator<in NodeDescriptor<*>>) {
    super.installComparator(builder, comparator)
  }

}
