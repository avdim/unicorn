// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import ru.tutu.idea.file.BaseProjectTreeBuilder2
import com.intellij.openapi.vcs.FileStatusListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import java.lang.Runnable
import com.intellij.openapi.application.ModalityState
import com.intellij.util.messages.MessageBusConnection
import com.intellij.openapi.application.ApplicationManager
import com.unicorn.Uni
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.ProjectTopics
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.ide.bookmarks.BookmarksListener
import javax.swing.tree.DefaultMutableTreeNode
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.psi.PsiManager
import ru.tutu.idea.file.ProjectViewPsiTreeChangeListener2
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ide.CopyPasteUtil
import com.intellij.openapi.project.Project
import com.intellij.problems.ProblemListener
import com.intellij.psi.PsiElement
import gnu.trove.THashSet

@Suppress("UnstableApiUsage")
open class ProjectTreeBuilder2(
    project: Project,
    tree: JTree,
    treeModel: DefaultTreeModel,
    treeStructure: ProjectAbstractTreeStructureBase
) : BaseProjectTreeBuilder2( /*project, */tree, treeModel, treeStructure) {
    private inner class MyFileStatusListener : FileStatusListener {
        override fun fileStatusesChanged() {
            queueUpdate(false)
        }

        override fun fileStatusChanged(vFile: VirtualFile) {
            queueUpdate(false)
        }
    }


    private class MyProblemListener : ProblemListener {
        private val myUpdateProblemAlarm = Alarm()
        private val myFilesToRefresh: MutableCollection<VirtualFile> = THashSet()
        override fun problemsAppeared(file: VirtualFile) {
            queueUpdate(file)
        }

        override fun problemsDisappeared(file: VirtualFile) {
            queueUpdate(file)
        }

        private fun queueUpdate(fileToRefresh: VirtualFile) {
            synchronized(myFilesToRefresh) {
                if (myFilesToRefresh.add(fileToRefresh)) {
                    myUpdateProblemAlarm.cancelAllRequests()
                    myUpdateProblemAlarm.addRequest({}, 200, ModalityState.NON_MODAL)
                }
            }
        }
    }

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(Uni)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                queueUpdate()
            }
        })
        connection.subscribe(BookmarksListener.TOPIC, object : BookmarksListener {})
        val rootNode = rootNode
        val updater = updater
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            object : ProjectViewPsiTreeChangeListener2(project) {
                override val rootNode: DefaultMutableTreeNode?
                    protected get() = rootNode
                override val updater: AbstractTreeUpdater?
                    protected get() = updater
                override val isFlattenPackages: Boolean
                    protected get() {
                        val structure = getTreeStructure()
                        return structure is AbstractProjectTreeStructure && structure.isFlattenPackages
                    }
            },
            this
        )
        FileStatusManager.getInstance(ProjectManager.getInstance().defaultProject).addFileStatusListener(
            MyFileStatusListener(), this
        )
        CopyPasteUtil.addDefaultListener(this, { element: PsiElement? ->
            addSubtreeToUpdateByElement(
                element!!
            )
        })
        connection.subscribe(ProblemListener.TOPIC, MyProblemListener())
        setCanYieldUpdate(true)
        initRootNode()
    }
}