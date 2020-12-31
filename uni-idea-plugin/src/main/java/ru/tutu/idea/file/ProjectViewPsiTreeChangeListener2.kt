/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.tutu.idea.file

import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import javax.swing.tree.DefaultMutableTreeNode
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.ObjectUtils

@Suppress("UnstableApiUsage")
abstract class ProjectViewPsiTreeChangeListener2 protected constructor(project: Project) : PsiTreeChangeListener {
    private val myModificationTracker: PsiModificationTracker
    private var myModificationCount: Long
    protected abstract val updater: AbstractTreeUpdater?
    protected abstract val isFlattenPackages: Boolean
    protected abstract val rootNode: DefaultMutableTreeNode?
    override fun childRemoved(event: PsiTreeChangeEvent) {
        val child = event.oldChild
        if (child is PsiWhiteSpace) return  //optimization
        childrenChanged(event.parent, true)
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        val child = event.newChild
        if (child is PsiWhiteSpace) return  //optimization
        childrenChanged(event.parent, true)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        val oldChild = event.oldChild
        val newChild = event.newChild
        if (oldChild is PsiWhiteSpace && newChild is PsiWhiteSpace) return  //optimization
        childrenChanged(event.parent, true)
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
        childrenChanged(event.oldParent, false)
        childrenChanged(event.newParent, true)
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        childrenChanged(event.parent, true)
    }

    protected fun childrenChanged(parent: PsiElement?, stopProcessingForThisModificationCount: Boolean) {
        var parent = parent
        if (parent is PsiDirectory && isFlattenPackages) {
            addSubtreeToUpdateByRoot()
            return
        }
        val newModificationCount = myModificationTracker.modificationCount
        if (newModificationCount == myModificationCount) return
        if (stopProcessingForThisModificationCount) {
            myModificationCount = newModificationCount
        }
        while (true) {
            if (parent == null) break
            if (parent is PsiFile) {
                val virtualFile = parent.virtualFile
                if (virtualFile != null && virtualFile.fileType !== FileTypes.PLAIN_TEXT) {
                    // adding a class within a file causes a new node to appear in project view => entire dir should be updated
                    parent = parent.containingDirectory
                    if (parent == null) break
                }
            } else if (parent is PsiDirectory &&
                ScratchUtil.isScratch(parent.virtualFile)
            ) {
                addSubtreeToUpdateByRoot()
                break
            }
            if (addSubtreeToUpdateByElementFile(parent)) {
                break
            }
            if (parent is PsiFile || parent is PsiDirectory) break
            parent = parent.parent
        }
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
        val propertyName = event.propertyName
        val element = event.element
        if (propertyName == PsiTreeChangeEvent.PROP_ROOTS) {
            addSubtreeToUpdateByRoot()
        } else if (propertyName == PsiTreeChangeEvent.PROP_WRITABLE) {
            if (!addSubtreeToUpdateByElementFile(element) && element is PsiFile) {
                addSubtreeToUpdateByElementFile(element.containingDirectory)
            }
        } else if (propertyName == PsiTreeChangeEvent.PROP_FILE_NAME || propertyName == PsiTreeChangeEvent.PROP_DIRECTORY_NAME) {
            if (element is PsiDirectory && isFlattenPackages) {
                addSubtreeToUpdateByRoot()
                return
            }
            val parent = element.parent
            if (parent == null || !addSubtreeToUpdateByElementFile(parent)) {
                addSubtreeToUpdateByElementFile(element)
            }
        } else if (propertyName == PsiTreeChangeEvent.PROP_FILE_TYPES || propertyName == PsiTreeChangeEvent.PROP_UNLOADED_PSI) {
            addSubtreeToUpdateByRoot()
        }
    }

    protected fun addSubtreeToUpdateByRoot() {
        val updater = updater
        val root = rootNode
        if (updater != null && root != null) updater.addSubtreeToUpdate(root)
    }

    protected fun addSubtreeToUpdateByElement(element: PsiElement): Boolean {
        val updater = updater
        return updater != null && updater.addSubtreeToUpdateByElement(element)
    }

    private fun addSubtreeToUpdateByElementFile(element: PsiElement?): Boolean {
        return element != null && addSubtreeToUpdateByElement(ObjectUtils.notNull(element.containingFile, element))
    }

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {}
    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {}
    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {}
    override fun beforeChildMovement(event: PsiTreeChangeEvent) {}
    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {}
    override fun beforePropertyChange(event: PsiTreeChangeEvent) {}

    init {
        myModificationTracker = PsiManager.getInstance(project).modificationTracker
        myModificationCount = myModificationTracker.modificationCount
    }
}