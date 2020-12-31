// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file;

import com.intellij.ProjectTopics;
import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.ide.projectView.ProjectViewPsiTreeChangeListener;
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.ProblemListener;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import com.unicorn.Uni;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;

@SuppressWarnings("UnstableApiUsage")
public class ProjectTreeBuilder2 extends BaseProjectTreeBuilder2 {
  public ProjectTreeBuilder2(@NotNull Project project,
                             @NotNull JTree tree,
                             @NotNull DefaultTreeModel treeModel,
                             @NotNull ProjectAbstractTreeStructureBase treeStructure) {
    super(/*project, */tree, treeModel, treeStructure);

    final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(Uni.INSTANCE);

    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        queueUpdate();
      }
    });

    connection.subscribe(BookmarksListener.TOPIC, new BookmarksListener() {});

    PsiManager.getInstance(project).addPsiTreeChangeListener(new ProjectTreeBuilderPsiListener(project), this);
    FileStatusManager.getInstance(ProjectManager.getInstance().getDefaultProject()).addFileStatusListener(new MyFileStatusListener(), this);
    CopyPasteUtil.addDefaultListener(this, this::addSubtreeToUpdateByElement);

    connection.subscribe(ProblemListener.TOPIC, new MyProblemListener());

    setCanYieldUpdate(true);

    initRootNode();
  }

  protected class ProjectTreeBuilderPsiListener extends ProjectViewPsiTreeChangeListener {
    public ProjectTreeBuilderPsiListener(final Project project) {
      super(project);
    }

    @Override
    protected DefaultMutableTreeNode getRootNode(){
      return ProjectTreeBuilder2.this.getRootNode();
    }

    @Override
    protected AbstractTreeUpdater getUpdater() {
      return ProjectTreeBuilder2.this.getUpdater();
    }

    @Override
    protected boolean isFlattenPackages(){
      AbstractTreeStructure structure = getTreeStructure();
      return structure instanceof AbstractProjectTreeStructure && ((AbstractProjectTreeStructure)structure).isFlattenPackages();
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      queueUpdate(false);
    }

    @Override
    public void fileStatusChanged(@NotNull VirtualFile vFile) {
       queueUpdate(false);
    }
  }

  private static class MyProblemListener implements ProblemListener {
    private final Alarm myUpdateProblemAlarm = new Alarm();
    private final Collection<VirtualFile> myFilesToRefresh = new THashSet<>();

    @Override
    public void problemsAppeared(@NotNull VirtualFile file) {
      queueUpdate(file);
    }

    @Override
    public void problemsDisappeared(@NotNull VirtualFile file) {
      queueUpdate(file);
    }

    private void queueUpdate(@NotNull VirtualFile fileToRefresh) {
      synchronized (myFilesToRefresh) {
        if (myFilesToRefresh.add(fileToRefresh)) {
          myUpdateProblemAlarm.cancelAllRequests();
          myUpdateProblemAlarm.addRequest(() -> {
            return;
//            if (!myProject.isOpen()) return;
//            Set<VirtualFile> filesToRefresh;
//            synchronized (myFilesToRefresh) {
//              filesToRefresh = new THashSet<>(myFilesToRefresh);
//            }
//            final DefaultMutableTreeNode rootNode = getRootNode();
//            if (rootNode != null) {
//              updateNodesContaining(filesToRefresh, rootNode);
//            }
//            synchronized (myFilesToRefresh) {
//              myFilesToRefresh.removeAll(filesToRefresh);
//            }
          }, 200, ModalityState.NON_MODAL);
        }
      }
    }
  }

}