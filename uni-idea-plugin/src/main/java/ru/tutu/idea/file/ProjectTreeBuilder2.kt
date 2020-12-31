// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file

import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.openapi.vcs.FileStatusListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ApplicationManager
import com.unicorn.Uni
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.ProjectTopics
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.ide.bookmarks.BookmarksListener
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ide.CopyPasteUtil
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileManagerListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.problems.ProblemListener
import com.intellij.psi.*
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ObjectUtils
import gnu.trove.THashSet

@Suppress("UnstableApiUsage")
open class ProjectTreeBuilder2(
  project: Project,
  tree: JTree,
  treeModel: DefaultTreeModel,
  treeStructure: ProjectAbstractTreeStructureBase
) : BaseProjectTreeBuilder2( /*project, */tree, treeModel, treeStructure) {

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

    fun addSubtreeToUpdateByRoot() {
      //этот код обновляет дерево целиком
      if (updater != null && rootNode != null) {
        updater.addSubtreeToUpdate(rootNode)
      }
    }

    fun addSubtreeToUpdateByElementFile(element: PsiElement?): Boolean {
      //todo этот код обновляет конкретные элементы в дереве. Я пока такую логику себе не затаскивал.
      fun addSubtreeToUpdateByElement(element: PsiElement): Boolean {
        return updater != null && updater.addSubtreeToUpdateByElement(element)
      }
      return element != null && addSubtreeToUpdateByElement(ObjectUtils.notNull(element.containingFile, element))
    }

    VirtualFileManager.getInstance().addAsyncFileListener(
      {
        object : AsyncFileListener.ChangeApplier {
          override fun afterVfsChange() {
//            Uni.log.debug { "afterVfsChange, $it" }
            addSubtreeToUpdateByRoot()
          }
        }
      },
      Uni
    )
    VirtualFileManager.getInstance().addVirtualFileManagerListener(
      object : VirtualFileManagerListener {
        override fun afterRefreshFinish(asynchronous: Boolean) {

        }

        override fun beforeRefreshStart(asynchronous: Boolean) {

        }
      },
      Uni
    )
    if (false) {
      // Это старый код, который завязан на Project
      PsiManager.getInstance(/*project*/ProjectManager.getInstance().defaultProject).addPsiTreeChangeListener(
        createOldListener(::addSubtreeToUpdateByRoot, ::addSubtreeToUpdateByElementFile), this
      )
    }
    FileStatusManager.getInstance(ProjectManager.getInstance().defaultProject).addFileStatusListener(
      object : FileStatusListener {
        override fun fileStatusesChanged() {
          queueUpdate(false)
        }

        override fun fileStatusChanged(vFile: VirtualFile) {
          queueUpdate(false)
        }
      },
      this
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

@Suppress("unused")
fun createOldListener(
  addSubtreeToUpdateByRoot: () -> Unit,
  addSubtreeToUpdateByElementFile: (element: PsiElement?) -> Boolean
) =
  object : PsiTreeChangeListener {
    //      val myModificationTracker: PsiModificationTracker = PsiManager.getInstance(project).modificationTracker
//      var myModificationCount: Long = myModificationTracker.modificationCount
    val isFlattenPackages: Boolean = true

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
//        val newModificationCount = myModificationTracker.modificationCount
//        if (newModificationCount == myModificationCount) return
//        if (stopProcessingForThisModificationCount) {
//          myModificationCount = newModificationCount
//        }
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

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {}
    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {}
    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {}
    override fun beforeChildMovement(event: PsiTreeChangeEvent) {}
    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {}
    override fun beforePropertyChange(event: PsiTreeChangeEvent) {}
  }
