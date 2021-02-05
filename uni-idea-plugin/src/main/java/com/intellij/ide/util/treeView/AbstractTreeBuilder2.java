// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util.treeView;

import com.intellij.my.file.AbstractProjectTreeStructure2;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Progressive;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.psi.impl.smartPointers.NodeDescriptor2;
import com.intellij.reference.SoftReference;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * @deprecated use {@link com.intellij.ui.tree.AsyncTreeModel} and {@link com.intellij.ui.tree.StructureTreeModel} instead.
 */
@SuppressWarnings("UnstableApiUsage")
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
@Deprecated
public class AbstractTreeBuilder2 implements Disposable {
  private AbstractTreeUi2 myUi;
  @NonNls private static final String TREE_BUILDER = "TreeBuilder";
  protected static final boolean DEFAULT_UPDATE_INACTIVE = true;

  protected AbstractTreeBuilder2() {

  }

  protected void init(@NotNull JTree tree,
                      @NotNull DefaultTreeModel treeModel,
                      AbstractProjectTreeStructure2 treeStructure) {

    tree.putClientProperty(TREE_BUILDER, new WeakReference<>(this));

    myUi = createUi();
    myUi.init(this, tree, treeModel, treeStructure);

    setPassthroughMode(isUnitTestingMode());
  }

  @NotNull
  protected AbstractTreeUi2 createUi() {
    return new AbstractTreeUi2();
  }

  public final void scrollTo(Object element, Runnable onDone) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.userScrollTo(element, onDone == null ? null : new UserRunnable(onDone));
  }

  public final void select(final Object element, @Nullable final Runnable onDone) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.userSelect(new Object[]{element}, new UserRunnable(onDone), false, true);
  }

  public final void expand(Object element, @Nullable Runnable onDone) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.expand(element, new UserRunnable(onDone));
  }

  public final void collapseChildren(Object element, @Nullable Runnable onDone) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.collapseChildren(element, new UserRunnable(onDone));
  }


  @Nullable
  protected AbstractTreeUpdater2 createUpdater() {
    if (isDisposed()) return null;

    AbstractTreeUpdater2 updater = new AbstractTreeUpdater2(this);
    updater.setModalityStateComponent(MergingUpdateQueue.ANY_COMPONENT);
    return updater;
  }

  @Nullable
  protected final AbstractTreeUpdater2 getUpdater() {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.getUpdater();
  }

  public final boolean addSubtreeToUpdateByElement(@NotNull Object element) {
    AbstractTreeUpdater2 updater = getUpdater();
    return updater != null && updater.addSubtreeToUpdateByElement(element);
  }

  @Nullable
  public final DefaultMutableTreeNode getRootNode() {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.getRootNode();
  }

  public final void setNodeDescriptorComparator(Comparator<? super NodeDescriptor2<?>> nodeDescriptorComparator) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) {
      ui.setNodeDescriptorComparator(nodeDescriptorComparator);
    }
  }

  /**
   * node descriptor getElement contract is as follows:
   * 1.TreeStructure always returns & receives "treeStructure" element returned by getTreeStructureElement
   * 2.Paths contain "model" element returned by getElement
   */
  protected Object getTreeStructureElement(NodeDescriptor2 nodeDescriptor) {
    return nodeDescriptor == null ? null : nodeDescriptor.getElement();
  }


  protected boolean validateNode(@NotNull Object child) {
    AbstractTreeStructure structure = getTreeStructure();
    return structure != null && structure.isValid(child);
  }

  public final JTree getTree() {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.getTree();
  }

  public final AbstractTreeStructure getTreeStructure() {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.getTreeStructure();
  }

  @Nullable
  public Object getRootElement() {
    AbstractTreeStructure structure = getTreeStructure();
    return structure == null ? null : structure.getRootElement();
  }

  /**
   * @deprecated use {@link #queueUpdate()}
   */
  @Deprecated
  public void updateFromRoot() {
    queueUpdate();
  }

  public void initRootNode() {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.initRootNode();
  }

  @NotNull
  public final ActionCallback queueUpdate() {
    return queueUpdate(true);
  }

  @NotNull
  public final ActionCallback queueUpdate(boolean withStructure) {
    return queueUpdateFrom(getRootElement(), true, withStructure);
  }

  public final void queueUpdateFrom(final Object element, final boolean forceResort) {
    queueUpdateFrom(element, forceResort, true);
  }

  @NotNull
  public ActionCallback queueUpdateFrom(final Object element, final boolean forceResort, final boolean updateStructure) {
    AbstractTreeUi2 ui = getUi();
    if (ui == null) {
      return ActionCallback.REJECTED;
    }

    final ActionCallback result = new ActionCallback();
    ui.invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeBuilder.queueUpdateFrom") {
      @Override
      public void perform() {
        AbstractTreeUi2 ui = getUi();
        if (ui == null) {
          result.reject("ui is null");
          return;
        }

        if (updateStructure && forceResort) {
          ui.incComparatorStamp();
        }
        ui.queueUpdate(element, updateStructure).notify(result);
      }
    });
    return result;
  }

  /**
   * @deprecated use {@link AbstractTreeUi#getNodeForElement(Object, boolean)}
   */
  @Deprecated
  @Nullable
  public DefaultMutableTreeNode getNodeForElement(@NotNull Object element) {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.getNodeForElement(element, false);
  }

  public void cleanUp() {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.doCleanUp();
  }

  @Nullable
  protected ProgressIndicator createProgressIndicator() {
    return null;
  }

  protected void expandNodeChildren(@NotNull DefaultMutableTreeNode node) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.doExpandNodeChildren(node);
  }

  protected boolean isAutoExpandNode(final NodeDescriptor2 nodeDescriptor) {
    return !isDisposed() && getRootElement() == getTreeStructureElement(nodeDescriptor);
  }

  protected boolean isAlwaysShowPlus(final NodeDescriptor2 descriptor) {
    return false;
  }


  protected boolean isSmartExpand() {
    return true;
  }

  public final boolean isDisposed() {
    return getUi() == null;
  }

  @Nullable
  protected Object findNodeByElement(@NotNull Object element) {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? null : ui.findNodeByElement(element);
  }

  void runOnYieldingDone(@NotNull Runnable onDone) {
    AbstractTreeUi2 ui = getUi();
    if (ui == null) return;

    if (ui.isPassthroughMode() || SwingUtilities.isEventDispatchThread()) {
      onDone.run();
    }
    else {
      EdtExecutorService.getInstance().execute(()->{
        if (!isDisposed()) onDone.run();
      });
    }
  }

  protected void yieldToEDT(@NotNull Runnable runnable) {
    AbstractTreeUi2 ui = getUi();
    if (ui == null) return;

    if (ui.isPassthroughMode()) {
      runnable.run();
    }
    else {
      EdtExecutorService.getInstance().execute(()->{
        if (!isDisposed()) runnable.run();
      });
    }
  }

  public boolean isToEnsureSelectionOnFocusGained() {
    return true;
  }

  protected void runBackgroundLoading(@NotNull final Runnable runnable) {
    if (isDisposed()) return;

    final Application app = ApplicationManager.getApplication();
    if (app != null) {
      app.runReadAction(new TreeRunnable2("AbstractTreeBuilder.runBackgroundLoading") {
        @Override
        public void perform() {
          runnable.run();
        }
      });
    }
    else {
      runnable.run();
    }
  }

  protected void updateAfterLoadedInBackground(@NotNull Runnable runnable) {
    AbstractTreeUi2 ui = getUi();
    if (ui == null) return;

    if (ui.isPassthroughMode()) {
      runnable.run();
    }
    else {
      UIUtil.invokeLaterIfNeeded(runnable);
    }
  }

  @NotNull
  public final ActionCallback getReady(Object requestor) {
    AbstractTreeUi2 ui = getUi();
    return ui == null ? ActionCallback.REJECTED : ui.getReady(requestor);
  }

  public void setPassthroughMode(boolean passthrough) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.setPassthroughMode(passthrough);
  }

  public void batch(@NotNull Progressive progressive) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) {
      ui.batch(progressive);
    }
  }

  @NotNull
  public Promise<Object> revalidateElement(@NotNull Object element) {
    AbstractTreeStructure structure = getTreeStructure();
    if (structure == null) {
      return Promises.rejectedPromise();
    }

    AsyncPromise<Object> promise = new AsyncPromise<>();
    structure
      .revalidateElement(element)
      .doWhenDone((Consumer<Object>)o -> promise.setResult(o))
      .doWhenRejected(s -> promise.setError(s));
    return promise;
  }

  public final AbstractTreeUi2 getUi() {
    AbstractTreeUi2 ui = myUi;
    return ui == null || ui.isReleaseRequested() ? null : ui;
  }

  @Override
  public void dispose() {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.requestRelease();
  }

  void releaseUi() {
    myUi = null;
  }

  protected boolean updateNodeDescriptor(@NotNull NodeDescriptor2 descriptor) {
    AbstractTreeUi2 ui = getUi();
    return ui != null && descriptor.update();
  }

  public final void setCanYieldUpdate(boolean yield) {
    AbstractTreeUi2 ui = getUi();
    if (ui != null) ui.setCanYield(yield);
  }

  @Nullable
  public static AbstractTreeBuilder2 getBuilderFor(@NotNull JTree tree) {
    Reference<AbstractTreeBuilder2> ref = (Reference)tree.getClientProperty(TREE_BUILDER);
    return SoftReference.dereference(ref);
  }

  private static boolean isUnitTestingMode() {
    Application app = ApplicationManager.getApplication();
    return app != null && app.isUnitTestMode();
  }

  public static boolean isToPaintSelection(@NotNull JTree tree) {
    AbstractTreeBuilder2 builder = getBuilderFor(tree);
    return builder == null || builder.getUi() == null || builder.getUi().isToPaintSelection();
  }

  class UserRunnable implements Runnable {
    private final Runnable myRunnable;

    UserRunnable(Runnable runnable) {
      myRunnable = runnable;
    }

    @Override
    public void run() {
      if (myRunnable != null) {
        AbstractTreeUi2 ui = getUi();
        if (ui != null) {
          ui.executeUserRunnable(myRunnable);
        }
        else {
          myRunnable.run();
        }
      }
    }
  }

}
