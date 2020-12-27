// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import com.intellij.ide.dnd.aware.DnDAwareTree
import javax.swing.tree.TreeCellRenderer
import com.intellij.ide.projectView.impl.ProjectViewRenderer
import javax.swing.tree.DefaultMutableTreeNode
import ru.tutu.idea.file.ProjectViewTree2
import java.lang.IllegalStateException
import ru.tutu.idea.file.ProjectViewTreeHelpers
import com.intellij.ui.DirtyUI
import com.intellij.psi.PsiElement
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.ui.popup.HintUpdateSupply
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Color
import javax.swing.tree.TreeModel

open class ProjectViewTree2(model: TreeModel?) : DnDAwareTree(null as TreeModel?) {
    /**
     * @return custom renderer for tree nodes
     */
    private fun createCellRenderer(): TreeCellRenderer {
        return ProjectViewRenderer()
    }

    override fun setToggleClickCount(count: Int) {
        if (count != 2) LOG.info(IllegalStateException("setToggleClickCount: unexpected count = $count"))
        super.setToggleClickCount(count)
    }

    override fun isFileColorsEnabled(): Boolean {
        return ProjectViewTreeHelpers.isFileColorsEnabledFor(this)
    }

    @DirtyUI
    override fun getFileColorFor(obj: Any): Color? {
        var obj: Any? = obj
        if (obj is DefaultMutableTreeNode) {
            obj = obj.userObject
        }
        if (obj is AbstractTreeNode<*>) {
            val value = obj.value
            if (value is PsiElement) {
                return ProjectViewTreeHelpers.getColorForElement(value)
            }
        }
        if (obj is ProjectViewNode<*>) {
            val node = obj
            val file = node.virtualFile
            if (file != null) {
                val project = node.project
                if (project != null && !project.isDisposed) {
                    val color = VfsPresentationUtil.getFileBackgroundColor(project, file)
                    if (color != null) return color
                }
            }
        }
        return null
    }

    companion object {
        private val LOG = Logger.getInstance(
            ProjectViewTree2::class.java
        )
    }

    init {
        isLargeModel = true
        setModel(model)
        setCellRenderer(createCellRenderer())
        HintUpdateSupply.installDataContextHintUpdateSupply(this)
    }
}