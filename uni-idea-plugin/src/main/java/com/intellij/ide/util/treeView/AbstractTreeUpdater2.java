// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util.treeView;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 * @deprecated use {@link com.intellij.ui.tree.AsyncTreeModel} and {@link com.intellij.ui.tree.StructureTreeModel} instead.
 */
@SuppressWarnings("UnstableApiUsage")
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
@Deprecated
public class AbstractTreeUpdater2 implements Disposable, Activatable {
  private static final Logger LOG = Logger.getInstance(AbstractTreeUpdater2.class);

  private final LinkedList<TreeUpdatePass2> myNodeQueue = new LinkedList<>();
  private final AbstractTreeBuilder2 myTreeBuilder;
  private final List<Runnable> myRunAfterUpdate = new ArrayList<>();
  private final MergingUpdateQueue myUpdateQueue;

  private long myUpdateCount;

  public AbstractTreeUpdater2(@NotNull AbstractTreeBuilder2 treeBuilder) {
    myTreeBuilder = treeBuilder;
    final JTree tree = myTreeBuilder.getTree();
    final JComponent component = tree instanceof TreeTableTree ? ((TreeTableTree)tree).getTreeTable() : tree;
    myUpdateQueue = new MergingUpdateQueue("UpdateQueue", 100, component.isShowing(), component);
    myUpdateQueue.setRestartTimerOnAdd(false);

    final UiNotifyConnector uiNotifyConnector = new UiNotifyConnector(component, myUpdateQueue);
    Disposer.register(this, myUpdateQueue);
    Disposer.register(this, uiNotifyConnector);
  }

  void setPassThroughMode(boolean passThroughMode) {
    myUpdateQueue.setPassThrough(passThroughMode);
  }

  void setModalityStateComponent(JComponent c) {
    myUpdateQueue.setModalityStateComponent(c);
  }

  public ModalityState getModalityState() {
    return myUpdateQueue.getModalityState();
  }

  public boolean hasNodesToUpdate() {
    return !myNodeQueue.isEmpty() || !myUpdateQueue.isEmpty();
  }

  @Override
  public void dispose() {
  }

  /**
   * @deprecated use {@link AbstractTreeBuilder#queueUpdateFrom(Object, boolean)}
   */
  @Deprecated
  public synchronized void addSubtreeToUpdate(@NotNull DefaultMutableTreeNode rootNode) {
    addSubtreeToUpdate(new TreeUpdatePass2(rootNode).setUpdateStamp(-1));
  }

  /**
   * @deprecated use {@link AbstractTreeBuilder#queueUpdateFrom(Object, boolean)}
   */
  @Deprecated
  synchronized void requeue(@NotNull TreeUpdatePass2 toAdd) {
    addSubtreeToUpdate(toAdd.setUpdateStamp(-1));
  }

  /**
   * @deprecated use {@link AbstractTreeBuilder#queueUpdateFrom(Object, boolean)}
   */
  @Deprecated
  synchronized void addSubtreeToUpdate(@NotNull TreeUpdatePass2 toAdd) {
    assert !toAdd.isExpired();

    final AbstractTreeUi2 ui = myTreeBuilder.getUi();
    if (ui == null) return;

    if (ui.isUpdatingChildrenNow(toAdd.getNode())) {
      toAdd.expire();
    }
    else {
      for (Iterator<TreeUpdatePass2> iterator = myNodeQueue.iterator(); iterator.hasNext();) {
        final TreeUpdatePass2 passInQueue = iterator.next();

        boolean isMatchingPass =
          toAdd.isUpdateStructure() == passInQueue.isUpdateStructure() && toAdd.isUpdateChildren() == passInQueue.isUpdateChildren();
        if (isMatchingPass) {
          if (passInQueue == toAdd) {
            toAdd.expire();
            break;
          }
          if (passInQueue.getNode() == toAdd.getNode()) {
            toAdd.expire();
            break;
          }
          if (toAdd.getNode().isNodeAncestor(passInQueue.getNode())) {
            toAdd.expire();
            break;
          }
          if (passInQueue.getNode().isNodeAncestor(toAdd.getNode())) {
            iterator.remove();
            passInQueue.expire();
          }
        }
      }
    }


    if (toAdd.getUpdateStamp() >= 0) {
      Object element = ui.getElementFor(toAdd.getNode());
      if ((element == null || !ui.isParentLoadingInBackground(element)) && !ui.isParentUpdatingChildrenNow(toAdd.getNode())) {
        toAdd.setUpdateStamp(-1);
      }
    }

    long newUpdateCount = toAdd.getUpdateStamp() == -1 ? myUpdateCount : myUpdateCount + 1;

    if (!toAdd.isExpired()) {
      final Collection<TreeUpdatePass2> yielding = ui.getYeildingPasses();
      for (TreeUpdatePass2 eachYielding : yielding) {
        final DefaultMutableTreeNode eachNode = eachYielding.getCurrentNode();
        if (eachNode != null) {
          if (eachNode.isNodeAncestor(toAdd.getNode())) {
            eachYielding.setUpdateStamp(newUpdateCount);
          }
        }
      }
    }


    if (toAdd.isExpired()) {
      reQueueViewUpdateIfNeeded();
      return;
    }


    myNodeQueue.add(toAdd);
    ui.addActivity();

    myUpdateCount = newUpdateCount;
    toAdd.setUpdateStamp(myUpdateCount);

    reQueueViewUpdate();
  }

  private void reQueueViewUpdateIfNeeded() {
    if (myUpdateQueue.isEmpty() && !myNodeQueue.isEmpty()) {
      reQueueViewUpdate();
    }
  }

  private void reQueueViewUpdate() {
    queue(new Update("ViewUpdate") {
      @Override
      public boolean isExpired() {
        return myTreeBuilder.isDisposed();
      }

      @Override
      public void run() {
        AbstractTreeStructure structure = myTreeBuilder.getTreeStructure();
        if (structure.hasSomethingToCommit()) {
          structure.asyncCommit().doWhenDone(new TreeRunnable2("AbstractTreeUpdater.reQueueViewUpdate") {
            @Override
            public void perform() {
              reQueueViewUpdateIfNeeded();
            }
          });
          return;
        }
        try {
          performUpdate();
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (RuntimeException e) {
          LOG.error(myTreeBuilder.getClass().getName(), e);
        }
      }
    });
  }

  private void queue(@NotNull Update update) {
    if (isReleased()) return;

    myUpdateQueue.queue(update);
  }

  public synchronized void performUpdate() {
    while (!myNodeQueue.isEmpty()) {
      if (isInPostponeMode()) break;

      final TreeUpdatePass2 eachPass = myNodeQueue.removeFirst();

      beforeUpdate().doWhenDone(new TreeRunnable2("AbstractTreeUpdater.performUpdate") {
        @Override
        public void perform() {
          try {
            AbstractTreeUi2 ui = myTreeBuilder.getUi();
            if (ui != null) ui.updateSubtreeNow(eachPass);
          }
          catch (ProcessCanceledException ignored) {
          }
        }
      });
    }

    AbstractTreeUi2 ui = myTreeBuilder.getUi();
    if (ui == null) return;

    ui.maybeReady();

    maybeRunAfterUpdate();
  }

  private void maybeRunAfterUpdate() {
    final Runnable runnable = new TreeRunnable2("AbstractTreeUpdater.maybeRunAfterUpdate") {
      @Override
      public void perform() {
        List<Runnable> runAfterUpdate = null;
        synchronized (myRunAfterUpdate) {
          if (!myRunAfterUpdate.isEmpty()) {
            runAfterUpdate = new ArrayList<>(myRunAfterUpdate);
            myRunAfterUpdate.clear();
          }
        }
        if (runAfterUpdate != null) {
          for (Runnable r : runAfterUpdate) {
            r.run();
          }
        }
      }
    };

    myTreeBuilder.getReady(this).doWhenDone(runnable);
  }

  private boolean isReleased() {
    return myTreeBuilder.getUi() == null;
  }

  protected ActionCallback beforeUpdate() {
    return ActionCallback.DONE;
  }

  /**
   * @deprecated use {@link AbstractTreeBuilder#queueUpdateFrom(Object, boolean)}
   */
  @Deprecated
  public boolean addSubtreeToUpdateByElement(@NotNull Object element) {
    DefaultMutableTreeNode node = myTreeBuilder.getNodeForElement(element);
    if (node != null) {
      myTreeBuilder.queueUpdateFrom(element, false);
      return true;
    }
    else {
      return false;
    }
  }

  public synchronized void cancelAllRequests() {
    myNodeQueue.clear();
    myUpdateQueue.cancelAllUpdates();
  }

  public void runAfterUpdate(final Runnable runnable) {
    if (runnable != null) {
      synchronized (myRunAfterUpdate) {
        myRunAfterUpdate.add(runnable);
      }
    }
  }

  public synchronized long getUpdateCount() {
    return myUpdateCount;
  }

  boolean isRerunNeededFor(TreeUpdatePass2 pass) {
    return pass.getUpdateStamp() < getUpdateCount();
  }

  boolean isInPostponeMode() {
    return !myUpdateQueue.isActive() && !myUpdateQueue.isPassThrough();
  }

  @Override
  public void showNotify() {
    myUpdateQueue.showNotify();
  }

  @Override
  public void hideNotify() {
    myUpdateQueue.hideNotify();
  }

  @NonNls
  @Override
  public synchronized String toString() {
    return "AbstractTreeUpdater updateCount=" + myUpdateCount + " queue=[" + myUpdateQueue + "] " + " nodeQueue=" + myNodeQueue + " builder=" + myTreeBuilder;
  }

  synchronized boolean isEnqueuedToUpdate(DefaultMutableTreeNode node) {
    for (TreeUpdatePass2 pass : myNodeQueue) {
      if (pass.willUpdate(node)) return true;
    }
    return false;
  }

  public void reset() {
    TreeUpdatePass2[] passes;
    synchronized (this) {
      passes = myNodeQueue.toArray(new TreeUpdatePass2[0]);
      myNodeQueue.clear();
    }
    myUpdateQueue.cancelAllUpdates();

    AbstractTreeUi2 ui = myTreeBuilder.getUi();
    if (ui != null) {
      for (TreeUpdatePass2 each : passes) {
        ui.addToCancelled(each.getNode());
      }
    }
  }
}
