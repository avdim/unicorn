// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.treeView;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.UiActivity;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.my.file.AbstractProjectTreeStructure2;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.impl.smartPointers.NodeDescriptor2;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.*;
import com.intellij.util.concurrency.LockToken;
import com.intellij.util.concurrency.QueueProcessor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.unicorn.Uni;
import gnu.trove.THashSet;
import org.jetbrains.annotations.*;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @deprecated use {@link com.intellij.ui.tree.AsyncTreeModel} and {@link com.intellij.ui.tree.StructureTreeModel} instead.
 */
@SuppressWarnings("UnstableApiUsage")
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
@Deprecated
public class AbstractTreeUi2 {
  private static final Logger LOG = Logger.getInstance(AbstractTreeBuilder2.class);
  protected JTree myTree;// protected for TestNG

  private DefaultTreeModel myTreeModel;
  private AbstractProjectTreeStructure2 myTreeStructure;
  private AbstractTreeUpdater2 myUpdater;

  private Comparator<? super NodeDescriptor2> myNodeDescriptorComparator;

  private final Comparator<TreeNode> myNodeComparator = new Comparator<TreeNode>() {
    @Override
    public int compare(TreeNode n1, TreeNode n2) {
      if (isLoadingNode(n1) && isLoadingNode(n2)) return 0;
      if (isLoadingNode(n1)) return -1;
      if (isLoadingNode(n2)) return 1;

      NodeDescriptor2 nodeDescriptor1 = getDescriptorFrom(n1);
      NodeDescriptor2 nodeDescriptor2 = getDescriptorFrom(n2);

      if (nodeDescriptor1 == null && nodeDescriptor2 == null) return 0;
      if (nodeDescriptor1 == null) return -1;
      if (nodeDescriptor2 == null) return 1;

      return myNodeDescriptorComparator != null
             ? myNodeDescriptorComparator.compare(nodeDescriptor1, nodeDescriptor2)
             : nodeDescriptor1.getIndex() - nodeDescriptor2.getIndex();
    }
  };

  long myOwnComparatorStamp;

  private DefaultMutableTreeNode myRootNode;
  private final Map<Object, Object> myElementToNodeMap = new HashMap<>();
  private final Set<DefaultMutableTreeNode> myUnbuiltNodes = new HashSet<>();
  private TreeExpansionListener myExpansionListener;
  private MySelectionListener mySelectionListener;

  private final QueueProcessor<Runnable> myWorker = new QueueProcessor<>(runnable -> {
    runnable.run();
    TimeoutUtil.sleep(1);
  });
  private final Set<Runnable> myActiveWorkerTasks = new HashSet<>();

  private ProgressIndicator myProgress;
  private boolean myRootNodeWasQueuedToInitialize;
  private boolean myRootNodeInitialized;

  private final Map<Object, List<NodeAction>> myNodeActions = new HashMap<>();
  private boolean myUpdateFromRootRequested;
  private boolean myWasEverShown;
  private boolean myUpdateIfInactive;

  private final Map<Object, UpdateInfo> myLoadedInBackground = new HashMap<>();
  private final Map<Object, List<NodeAction>> myNodeChildrenActions = new HashMap<>();

  private long myClearOnHideDelay = -1;
  private volatile long ourUi2Countdown;

  private final Set<Runnable> myDeferredSelections = new HashSet<>();
  private final Set<Runnable> myDeferredExpansions = new HashSet<>();

  private boolean myCanProcessDeferredSelections;

  private UpdaterTreeState2 myUpdaterState;
  private AbstractTreeBuilder2 myBuilder;

  private final Set<DefaultMutableTreeNode> myUpdatingChildren = new THashSet<>();

  private boolean myCanYield;

  private final List<TreeUpdatePass2> myYieldingPasses = new ArrayList<>();

  private boolean myYieldingNow;

  private final Set<DefaultMutableTreeNode> myPendingNodeActions = new HashSet<>();
  private final Set<Runnable> myYieldingDoneRunnables = new HashSet<>();

  private final Alarm myBusyAlarm = new Alarm();
  private final Runnable myWaiterForReady = new TreeRunnable2("AbstractTreeUi.myWaiterForReady") {
    @Override
    public void perform() {
      maybeSetBusyAndScheduleWaiterForReady(false, null);
    }
  };

  private final RegistryValue myYieldingUpdate = Registry.get("ide.tree.yieldingUiUpdate");
  private final RegistryValue myShowBusyIndicator = Registry.get("ide.tree.showBusyIndicator");
  private final RegistryValue myWaitForReadyTime = Registry.get("ide.tree.waitForReadyTimeout");

  private boolean myWasEverIndexNotReady;
  private boolean myShowing;
  private final FocusAdapter myFocusListener = new FocusAdapter() {
    @Override
    public void focusGained(FocusEvent e) {
      maybeReady();
    }
  };
  private final Set<DefaultMutableTreeNode> myNotForSmartExpand = new HashSet<>();
  private TreePath myRequestedExpand;

  private TreePath mySilentExpand;
  private TreePath mySilentSelect;

  private final ActionCallback myInitialized = new ActionCallback();
  private final BusyObject.Impl myBusyObject = new BusyObject.Impl() {
    @Override
    public boolean isReady() {
      return AbstractTreeUi2.this.isReady(true);
    }

    @Override
    protected void onReadyWasSent() {
      removeActivity();
    }
  };

  private boolean myPassThroughMode;

  private final Set<DefaultMutableTreeNode> myWillBeExpanded = new HashSet<>();
  private SimpleTimerTask myCleanupTask;

  private final AtomicBoolean myCancelRequest = new AtomicBoolean();
  private final ReentrantLock myStateLock = new ReentrantLock();

  private final AtomicBoolean myResettingToReadyNow = new AtomicBoolean();

  private final Map<Progressive, ProgressIndicator> myBatchIndicators = new HashMap<>();
  private final Map<Progressive, ActionCallback> myBatchCallbacks = new HashMap<>();

  private final Map<DefaultMutableTreeNode, DefaultMutableTreeNode> myCancelledBuild = new WeakHashMap<>();

  private boolean mySelectionIsAdjusted;
  private boolean myReleaseRequested;

  private final Set<Object> myRevalidatedObjects = new HashSet<>();

  private final Set<Runnable> myUserRunnables = new HashSet<>();

  private UiActivityMonitor myActivityMonitor;
  @NonNls private UiActivity myActivityId;

  @Override
  public String toString() {
    return "AbstractTreeUi: builder = " + myBuilder;
  }

  protected void init(@NotNull AbstractTreeBuilder2 builder,
                      @NotNull JTree tree,
                      @NotNull DefaultTreeModel treeModel,
                      AbstractProjectTreeStructure2 treeStructure) {
    myBuilder = builder;
    myTree = tree;
    myTreeModel = treeModel;
    myActivityMonitor = UiActivityMonitor.getInstance();
    myActivityId = new UiActivity.AsyncBgOperation("TreeUi " + this);
    addModelListenerToDiagnoseAccessOutsideEdt();
    myTree.setModel(myTreeModel);
    setRootNode((DefaultMutableTreeNode)treeModel.getRoot());
    myTreeStructure = treeStructure;
    myNodeDescriptorComparator = null;
    myUpdateIfInactive = AbstractTreeBuilder2.DEFAULT_UPDATE_INACTIVE;

    UIUtil.invokeLaterIfNeeded(new TreeRunnable2("AbstractTreeUi.init") {
      @Override
      public void perform() {
        if (!wasRootNodeInitialized()) {
          if (myRootNode.getChildCount() == 0) {
            insertLoadingNode(myRootNode, true);
          }
        }
      }
    });

    myExpansionListener = new MyExpansionListener();
    myTree.addTreeExpansionListener(myExpansionListener);

    mySelectionListener = new MySelectionListener();
    myTree.addTreeSelectionListener(mySelectionListener);

    setUpdater(getBuilder().createUpdater());
    myProgress = getBuilder().createProgressIndicator();
    Disposer.register(getBuilder(), getUpdater());
    if (myProgress != null) {
      Disposer.register(getBuilder(), () -> myProgress.cancel());
    }

    final UiNotifyConnector uiNotify = new UiNotifyConnector(tree, new Activatable() {
      @Override
      public void showNotify() {
        myShowing = true;
        myWasEverShown = true;
        if (canInitiateNewActivity()) {
          activate(true);
        }
      }

      @Override
      public void hideNotify() {
        myShowing = false;
        if (canInitiateNewActivity()) {
          deactivate();
        }
      }
    });
    Disposer.register(getBuilder(), uiNotify);

    myTree.addFocusListener(myFocusListener);
  }


  private boolean isNodeActionsPending() {
    return !myNodeActions.isEmpty() || !myNodeChildrenActions.isEmpty();
  }

  private void clearNodeActions() {
    myNodeActions.clear();
    myNodeChildrenActions.clear();
  }

  private void maybeSetBusyAndScheduleWaiterForReady(boolean forcedBusy, @Nullable Object element) {
    if (!myShowBusyIndicator.asBoolean()) return;

    boolean canUpdateBusyState = false;

    if (forcedBusy) {
      if (canYield() || isToBuildChildrenInBackground(element)) {
        canUpdateBusyState = true;
      }
    } else {
      canUpdateBusyState = true;
    }

    if (!canUpdateBusyState) return;

    if (myTree instanceof Tree) {
      final Tree tree = (Tree)myTree;
      final boolean isBusy = !isReady(true) || forcedBusy;
      if (isBusy && tree.isShowing()) {
        tree.setPaintBusy(true);
        myBusyAlarm.cancelAllRequests();
        myBusyAlarm.addRequest(myWaiterForReady, myWaitForReadyTime.asInteger());
      }
      else {
        tree.setPaintBusy(false);
      }
    }
  }

  private void setHoldSize(boolean holdSize) {
    if (myTree instanceof Tree) {
      final Tree tree = (Tree)myTree;
      tree.setHoldSize(holdSize);
    }
  }

  private void cleanUpAll() {
    final long now = System.currentTimeMillis();
    final long timeToCleanup = ourUi2Countdown;
    if (timeToCleanup != 0 && now >= timeToCleanup) {
      ourUi2Countdown = 0;
      Runnable runnable = new TreeRunnable2("AbstractTreeUi.cleanUpAll") {
        @Override
        public void perform() {
          if (!canInitiateNewActivity()) return;

          myCleanupTask = null;
          getBuilder().cleanUp();
        }
      };
      if (isPassthroughMode()) {
        runnable.run();
      }
      else {
        invokeLaterIfNeeded(false, runnable);
      }
    }
  }

  void doCleanUp() {
    Runnable cleanup = new TreeRunnable2("AbstractTreeUi.doCleanUp") {
      @Override
      public void perform() {
        if (canInitiateNewActivity()) {
          cleanUpNow();
        }
      }
    };

    if (isPassthroughMode()) {
      cleanup.run();
    }
    else {
      invokeLaterIfNeeded(false, cleanup);
    }
  }

  void invokeLaterIfNeeded(boolean forceEdt, @NotNull final Runnable runnable) {
    Runnable actual = new TreeRunnable2("AbstractTreeUi.invokeLaterIfNeeded") {
      @Override
      public void perform() {
        if (!isReleased()) {
          runnable.run();
        }
      }
    };

    if (isPassthroughMode() || !forceEdt && !isEdt() && !isTreeShowing() && !myWasEverShown) {
      actual.run();
    }
    else {
      UIUtil.invokeLaterIfNeeded(actual);
    }
  }

  public void activate(boolean byShowing) {
    cancelCurrentCleanupTask();

    myCanProcessDeferredSelections = true;
    ourUi2Countdown = 0;

    if (!myWasEverShown || myUpdateFromRootRequested || myUpdateIfInactive) {
      getBuilder().updateFromRoot();
    }

    getUpdater().showNotify();

    myWasEverShown |= byShowing;
  }

  private void cancelCurrentCleanupTask() {
    if (myCleanupTask != null) {
      myCleanupTask.cancel();
      myCleanupTask = null;
    }
  }

  void deactivate() {
    getUpdater().hideNotify();
    myBusyAlarm.cancelAllRequests();

    if (!myWasEverShown) return;
    // ask for termination of background children calculation
    if (myProgress != null && myProgress.isRunning()) myProgress.cancel();
    if (!isReady()) {
      cancelUpdate();
      myUpdateFromRootRequested = true;
    }

    if (getClearOnHideDelay() >= 0) {
      ourUi2Countdown = System.currentTimeMillis() + getClearOnHideDelay();
      scheduleCleanUpAll();
    }
  }

  private void scheduleCleanUpAll() {
    cancelCurrentCleanupTask();

    myCleanupTask = SimpleTimer.getInstance().setUp(new TreeRunnable2("AbstractTreeUi.scheduleCleanUpAll") {
      @Override
      public void perform() {
        cleanUpAll();
      }
    }, getClearOnHideDelay());
  }

  void requestRelease() {
    myReleaseRequested = true;
    cancelUpdate().doWhenDone(new TreeRunnable2("AbstractTreeUi.requestRelease: on done") {
      @Override
      public void perform() {
        releaseNow();
      }
    });
  }

  private void releaseNow() {
    try (LockToken ignored = acquireLock()) {
      myTree.removeTreeExpansionListener(myExpansionListener);
      myTree.removeTreeSelectionListener(mySelectionListener);
      myTree.removeFocusListener(myFocusListener);

      disposeNode(getRootNode());
      myElementToNodeMap.clear();
      getUpdater().cancelAllRequests();
      myWorker.clear();
      clearWorkerTasks();
      if (myProgress != null) {
        myProgress.cancel();
      }

      cancelCurrentCleanupTask();

      myTree = null;
      setUpdater(null);
      myTreeStructure = null;
      myBuilder.releaseUi();
      myBuilder = null;

      clearNodeActions();

      myDeferredSelections.clear();
      myDeferredExpansions.clear();
      myYieldingDoneRunnables.clear();
    }
  }

  public boolean isReleased() {
    return myBuilder == null;
  }

  void doExpandNodeChildren(@NotNull final DefaultMutableTreeNode node) {
    if (!myUnbuiltNodes.contains(node)) return;
    if (isLoadedInBackground(getElementFor(node))) return;

    AbstractTreeStructure structure = getTreeStructure();
    structure.asyncCommit().doWhenDone(new TreeRunnable2("AbstractTreeUi.doExpandNodeChildren") {
      @Override
      public void perform() {
        addSubtreeToUpdate(node);
        // at this point some tree updates may already have been run as a result of
        // in tests these updates may lead to the instance disposal, so getUpdater() at the next line may return null
        final AbstractTreeUpdater2 updater = getUpdater();
        if (updater != null) {
          updater.performUpdate();
        }
      }
    });
    //if (structure.hasSomethingToCommit()) structure.commit();
  }

  public final AbstractProjectTreeStructure2 getTreeStructure() {
    return myTreeStructure;
  }

  public final JTree getTree() {
    return myTree;
  }

  @Nullable
  private static AbstractTreeNod2 getDescriptorFrom(Object node) {
    if (node instanceof DefaultMutableTreeNode) {
      Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof NodeDescriptor2) {
        return (AbstractTreeNod2)userObject;
      }
    }

    return null;
  }

  @Nullable
  public final DefaultMutableTreeNode getNodeForElement(@NotNull Object element, final boolean validateAgainstStructure) {
    DefaultMutableTreeNode result = null;
    if (validateAgainstStructure) {
      int index = 0;
      while (true) {
        final DefaultMutableTreeNode node = findNode(element, index);
        if (node == null) break;

        if (isNodeValidForElement(element, node)) {
          result = node;
          break;
        }

        index++;
      }
    }
    else {
      result = getFirstNode(element);
    }


    if (result != null && !isNodeInStructure(result)) {
      disposeNode(result);
      result = null;
    }

    return result;
  }

  private boolean isNodeInStructure(@NotNull DefaultMutableTreeNode node) {
    return TreeUtil.isAncestor(getRootNode(), node) && getRootNode() == myTreeModel.getRoot();
  }

  private boolean isNodeValidForElement(@NotNull final Object element, @NotNull final DefaultMutableTreeNode node) {
    return isSameHierarchy(element, node) || isValidChildOfParent(element, node);
  }

  private boolean isValidChildOfParent(@NotNull final Object element, @NotNull final DefaultMutableTreeNode node) {
    final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
    final Object parentElement = getElementFor(parent);
    if (!isInStructure(parentElement)) return false;

    if (parent instanceof ElementNode) {
      return ((ElementNode)parent).isValidChild(element);
    }
    for (int i = 0; i < parent.getChildCount(); i++) {
      final TreeNode child = parent.getChildAt(i);
      final Object eachElement = getElementFor(child);
      if (element.equals(eachElement)) return true;
    }

    return false;
  }

  private boolean isSameHierarchy(@NotNull  Object element, @NotNull DefaultMutableTreeNode node) {
    Object eachParent = element;
    DefaultMutableTreeNode eachParentNode = node;
    boolean valid;
    while (true) {
      if (eachParent == null) {
        valid = eachParentNode == null;
        break;
      }

      if (!eachParent.equals(getElementFor(eachParentNode))) {
        valid = false;
        break;
      }

      eachParent = getTreeStructure().getParentElement(eachParent);
      eachParentNode = (DefaultMutableTreeNode)eachParentNode.getParent();
    }
    return valid;
  }

  public final void setNodeDescriptorComparator(Comparator<? super NodeDescriptor2> nodeDescriptorComparator) {
    myNodeDescriptorComparator = nodeDescriptorComparator;
    getBuilder().queueUpdateFrom(getTreeStructure().getRootElement(), true);
  }

  @NotNull
  protected AbstractTreeBuilder2 getBuilder() {
    return myBuilder;
  }

  protected final void initRootNode() {
    if (myUpdateIfInactive) {
      activate(false);
    }
    else {
      myUpdateFromRootRequested = true;
    }
  }

  private boolean initRootNodeNowIfNeeded(@NotNull final TreeUpdatePass2 pass) {
    boolean wasCleanedUp = false;
    if (myRootNodeWasQueuedToInitialize) {
      Object root = getTreeStructure().getRootElement();

      Object currentRoot = getElementFor(myRootNode);

      if (Comparing.equal(root, currentRoot)) return false;

      Object rootAgain = getTreeStructure().getRootElement();
      if (root != rootAgain && !root.equals(rootAgain)) {
        assert false : "getRootElement() if called twice must return either root1 == root2 or root1.equals(root2)";
      }

      cleanUpNow();
      wasCleanedUp = true;
    }

    if (myRootNodeWasQueuedToInitialize) return true;

    myRootNodeWasQueuedToInitialize = true;

    final Object rootElement = getTreeStructure().getRootElement();
    addNodeAction(rootElement, false, node -> processDeferredActions());


    final Ref<NodeDescriptor2> rootDescriptor = new Ref<>(null);
    final boolean bgLoading = isToBuildChildrenInBackground(rootElement);

    Runnable build = new TreeRunnable2("AbstractTreeUi.initRootNodeNowIfNeeded: build") {
      @Override
      public void perform() {
        rootDescriptor.set((NodeDescriptor2) rootElement);
        getRootNode().setUserObject(rootDescriptor.get());
        update(rootDescriptor.get(), true);
        pass.addToUpdated(rootDescriptor.get());
      }
    };


    Runnable update = new TreeRunnable2("AbstractTreeUi.initRootNodeNowIfNeeded: update") {
      @Override
      public void perform() {
        Object fromDescriptor = getElementFromDescriptor(rootDescriptor.get());
        if (!isNodeNull(fromDescriptor)) {
          createMapping(fromDescriptor, getRootNode());
        }


        insertLoadingNode(getRootNode(), true);

        boolean willUpdate = false;
        if (!rootDescriptor.isNull()) {
          willUpdate = myUnbuiltNodes.contains(getRootNode());
          expand(getRootNode(), true);
        }
        ActionCallback callback;
        if (willUpdate) {
          callback = ActionCallback.DONE;
        }
        else {
          callback = updateNodeChildren(getRootNode(), pass, null, false, false, false, true, true);
        }
        callback.doWhenDone(new TreeRunnable2("AbstractTreeUi.initRootNodeNowIfNeeded: on done updateNodeChildren") {
          @Override
          public void perform() {
            if (getRootNode().getChildCount() == 0) {
              myTreeModel.nodeChanged(getRootNode());
            }
          }
        });
      }
    };

    if (bgLoading) {
      queueToBackground(build, update)
        .onSuccess(new TreeConsumer2<Void>("AbstractTreeUi.initRootNodeNowIfNeeded: on processed queueToBackground") {
          @Override
          public void perform() {
            invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.initRootNodeNowIfNeeded: on processed queueToBackground later") {
              @Override
              public void perform() {
                myRootNodeInitialized = true;
                processNodeActionsIfReady(myRootNode);
              }
            });
          }
        });
    }
    else {
      build.run();
      update.run();
      myRootNodeInitialized = true;
      processNodeActionsIfReady(myRootNode);
    }

    return wasCleanedUp;
  }

  @NotNull
  private Promise<Boolean> update(@NotNull final NodeDescriptor2 nodeDescriptor, boolean now) {
    Promise<Boolean> promise;
    if (now || isPassthroughMode()) {
      promise = Promises.resolvedPromise(update(nodeDescriptor));
    }
    else {
      final AsyncPromise<Boolean> result = new AsyncPromise<>();
      promise = result;

      boolean bgLoading = isToBuildInBackground(nodeDescriptor);

      boolean edt = isEdt();
      if (bgLoading) {
        if (edt) {
          final AtomicBoolean changes = new AtomicBoolean();
          queueToBackground(new TreeRunnable2("AbstractTreeUi.update: build") {
                              @Override
                              public void perform() {
                                changes.set(update(nodeDescriptor));
                              }
                            }, new TreeRunnable2("AbstractTreeUi.update: post") {
                              @Override
                              public void perform() {
                                result.setResult(changes.get());
                              }
                            }
          );
        }
        else {
          result.setResult(update(nodeDescriptor));
        }
      }
      else {
        if (edt || !myWasEverShown) {
          result.setResult(update(nodeDescriptor));
        }
        else {
          invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.update: later") {
            @Override
            public void perform() {
              execute(new TreeRunnable2("AbstractTreeUi.update: later execute") {
                @Override
                public void perform() {
                  result.setResult(update(nodeDescriptor));
                }
              });
            }
          });
        }
      }
    }

    promise.onSuccess(changes -> {
      if (!changes) {
        return;
      }

      invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.update: on done result") {
        @Override
        public void perform() {
          Object element = nodeDescriptor.getElement();
          DefaultMutableTreeNode node = element == null ? null : getNodeForElement(element, false);
          if (node != null) {
            TreePath path = getPathFor(node);
            if (myTree.isVisible(path)) {
              updateNodeImageAndPosition(node);
            }
          }
        }
      });
    });
    return promise;
  }

  private boolean update(@NotNull final NodeDescriptor2 nodeDescriptor) {
    while(true) {
      try (LockToken ignored = attemptLock()) {
        if (ignored == null) {  // async children calculation is in progress under lock
          if (myProgress != null && myProgress.isRunning()) myProgress.cancel();
          continue;
        }
        final AtomicBoolean update = new AtomicBoolean();
        execute(new TreeRunnable2("AbstractTreeUi.update") {
          @Override
          public void perform() {
            nodeDescriptor.setUpdateCount(nodeDescriptor.getUpdateCount() + 1);
            update.set(getBuilder().updateNodeDescriptor(nodeDescriptor));
          }
        });
        return update.get();
      }
      catch (IndexNotReadyException e) {
        warnOnIndexNotReady(e);
        return false;
      }
      catch (InterruptedException e) {
        LOG.info(e);
        return false;
      }
    }
  }

  public void assertIsDispatchThread() {
    if (isPassthroughMode()) return;

    if ((isTreeShowing() || myWasEverShown) && !isEdt()) {
      LOG.error("Must be in event-dispatch thread");
    }
  }

  private static boolean isEdt() {
    return SwingUtilities.isEventDispatchThread();
  }

  private boolean isTreeShowing() {
    return myShowing;
  }

  private void assertNotDispatchThread() {
    if (isPassthroughMode()) return;

    if (isEdt()) {
      LOG.error("Must not be in event-dispatch thread");
    }
  }

  private void processDeferredActions() {
    processDeferredActions(myDeferredSelections);
    processDeferredActions(myDeferredExpansions);
  }

  private static void processDeferredActions(@NotNull Set<Runnable> actions) {
    final Runnable[] runnables = actions.toArray(new Runnable[0]);
    actions.clear();
    for (Runnable runnable : runnables) {
      runnable.run();
    }
  }

  //todo: to make real callback
  public void queueUpdate(Object element) {
    queueUpdate(element, true);
  }

  @NotNull
  public ActionCallback queueUpdate(final Object fromElement, boolean updateStructure) {
    assertIsDispatchThread();

    try {
      if (getUpdater() == null) {
        return ActionCallback.REJECTED;
      }

      final ActionCallback result = new ActionCallback();
      DefaultMutableTreeNode nodeToUpdate = null;
      boolean updateElementStructure = updateStructure;
      for (Object element = fromElement; element != null; element = getTreeStructure().getParentElement(element)) {
        final DefaultMutableTreeNode node = getFirstNode(element);
        if (node != null) {
          nodeToUpdate = node;
          break;
        }
        updateElementStructure = true; // always update children if element does not exist
      }

      addSubtreeToUpdate(nodeToUpdate != null? nodeToUpdate : getRootNode(), new TreeRunnable2("AbstractTreeUi.queueUpdate") {
        @Override
        public void perform() {
          result.setDone();
        }
      }, updateElementStructure);

      return result;
    }
    catch (ProcessCanceledException e) {
      return ActionCallback.REJECTED;
    }
  }

  final void updateSubtreeNow(@NotNull TreeUpdatePass2 pass) {
    maybeSetBusyAndScheduleWaiterForReady(true, getElementFor(pass.getNode()));
    setHoldSize(true);

    boolean consumed = initRootNodeNowIfNeeded(pass);
    if (consumed) return;

    final DefaultMutableTreeNode node = pass.getNode();

    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;

    if (pass.isUpdateStructure()) {
      setUpdaterState(new UpdaterTreeState2(this)).beforeSubtreeUpdate();

      boolean forceUpdate = true;
      TreePath path = getPathFor(node);
      boolean invisible = !myTree.isExpanded(path) && (path.getParentPath() == null || !myTree.isExpanded(path.getParentPath()));

      if (invisible && myUnbuiltNodes.contains(node)) {
        forceUpdate = false;
      }

      updateNodeChildren(node, pass, null, false, false, forceUpdate, false, pass.isUpdateChildren());
    }
    else {
      updateRow(0, pass);
    }
  }

  private void updateRow(final int row, @NotNull final TreeUpdatePass2 pass) {
    LOG.debug("updateRow: ", row, " - ", pass);
    invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.updateRow") {
      @Override
      public void perform() {
        if (row >= getTree().getRowCount()) return;

        TreePath path = getTree().getPathForRow(row);
        if (path != null) {
          final NodeDescriptor2 descriptor = getDescriptorFrom(path.getLastPathComponent());
          if (descriptor != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            maybeYield(() -> update(descriptor, false)
              .onSuccess(new TreeConsumer2<Boolean>("AbstractTreeUi.updateRow: inner") {
                @Override
                public void perform() {
                  updateRow(row + 1, pass);
                }
              }), pass, node);
          }
        }
      }
    });
  }

  boolean isToBuildChildrenInBackground(Object element) {
    AbstractTreeStructure structure = getTreeStructure();
    return element != null && structure.isToBuildChildrenInBackground(element);
  }

  private boolean isToBuildInBackground(NodeDescriptor2 descriptor) {
    return isToBuildChildrenInBackground(getElementFromDescriptor(descriptor));
  }

  @NotNull
  private UpdaterTreeState2 setUpdaterState(@NotNull UpdaterTreeState2 state) {
    if (state.equals(myUpdaterState)) return state;

    final UpdaterTreeState2 oldState = myUpdaterState;
    if (oldState == null) {
      myUpdaterState = state;
      return state;
    }
    else {
      oldState.addAll(state);
      return oldState;
    }
  }

  void doUpdateNode(@NotNull final DefaultMutableTreeNode node) {
    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;
    final Object prevElement = getElementFromDescriptor(descriptor);
    if (prevElement == null) return;
    update(descriptor, false)
      .onSuccess(changes -> {
        if (!isValid(descriptor)) {
          if (isInStructure(prevElement)) {
            Object toUpdate = ObjectUtils.notNull(getTreeStructure().getParentElement(prevElement), getTreeStructure().getRootElement());
            getUpdater().addSubtreeToUpdateByElement(toUpdate);
            return;
          }
        }
        if (changes) {
          updateNodeImageAndPosition(node);
        }
      });
  }

  public AbstractTreeNod2 getElementFromDescriptor(NodeDescriptor2 descriptor) {
    return getBuilder().getTreeStructureElement(descriptor);
  }

  @NotNull
  private ActionCallback updateNodeChildren(@NotNull final DefaultMutableTreeNode node,
                                            @NotNull final TreeUpdatePass2 pass,
                                            @Nullable final LoadedChildren loadedChildren,
                                            final boolean forcedNow,
                                            final boolean toSmartExpand,
                                            final boolean forceUpdate,
                                            final boolean descriptorIsUpToDate,
                                            final boolean updateChildren) {
    AbstractTreeStructure treeStructure = getTreeStructure();
    ActionCallback result = treeStructure.asyncCommit();
    result.doWhenDone(new TreeRunnable2("AbstractTreeUi.updateNodeChildren: on done") {
      @Override
      public void perform() {
        try {
          removeFromCancelled(node);
          execute(new TreeRunnable2("AbstractTreeUi.updateNodeChildren: execute") {
            @Override
            public void perform() {
              doUpdateChildren(node, pass, loadedChildren, forcedNow, toSmartExpand, forceUpdate, descriptorIsUpToDate, updateChildren);
            }
          });
        }
        catch (ProcessCanceledException e) {
          addToCancelled(node);
          throw e;
        }
      }
    });

    return result;
  }

  private void doUpdateChildren(@NotNull final DefaultMutableTreeNode node,
                                @NotNull final TreeUpdatePass2 pass,
                                @Nullable final LoadedChildren loadedChildren,
                                boolean forcedNow,
                                final boolean toSmartExpand,
                                boolean forceUpdate,
                                boolean descriptorIsUpToDate,
                                final boolean updateChildren) {
    try {

      final AbstractTreeNod2 descriptor = getDescriptorFrom(node);
      if (descriptor == null) {
        removeFromUnbuilt(node);
        removeLoading(node, true);
        return;
      }

      boolean descriptorIsReady = descriptorIsUpToDate || pass.isUpdated(descriptor);

      final boolean wasExpanded = myTree.isExpanded(new TreePath(node.getPath()));
      final boolean wasLeaf = node.getChildCount() == 0;


      boolean bgBuild = isToBuildInBackground(descriptor);
      boolean requiredToUpdateChildren = forcedNow || wasExpanded;

      if (!requiredToUpdateChildren && forceUpdate) {
        boolean alwaysPlus = getBuilder().isAlwaysShowPlus(descriptor);
        if (alwaysPlus && wasLeaf) {
          requiredToUpdateChildren = true;
        }
        else {
          requiredToUpdateChildren = !alwaysPlus;
          if (!requiredToUpdateChildren && !myUnbuiltNodes.contains(node)) {
            removeChildren(node);
          }
        }
      }

      final AtomicReference<LoadedChildren> preloaded = new AtomicReference<>(loadedChildren);

      if (!requiredToUpdateChildren) {
        if (myUnbuiltNodes.contains(node) && node.getChildCount() == 0) {
          insertLoadingNode(node, true);
        }

        if (!descriptorIsReady) {
          update(descriptor, false);
        }

        return;
      }

      if (!forcedNow && !bgBuild && myUnbuiltNodes.contains(node)) {
        if (!descriptorIsReady) {
          update(descriptor, true);
          descriptorIsReady = true;
        }

        if (processAlwaysLeaf(node) || !updateChildren) {
          return;
        }

        Pair<Boolean, LoadedChildren> unbuilt = processUnbuilt(node, descriptor, pass, wasExpanded, null);

        if (unbuilt.getFirst()) {
          return;
        }
        preloaded.set(unbuilt.getSecond());
      }


      final boolean childForceUpdate = isChildNodeForceUpdate(node, forceUpdate, wasExpanded);

      if (!forcedNow && isToBuildInBackground(descriptor)) {
        boolean alwaysLeaf = processAlwaysLeaf(node);
        queueBackgroundUpdate(
          new UpdateInfo(descriptor, pass, canSmartExpand(node, toSmartExpand), wasExpanded, childForceUpdate, descriptorIsReady,
                         !alwaysLeaf && updateChildren), node);
      }
      else {
        if (!descriptorIsReady) {
          update(descriptor, false)
            .onSuccess(new TreeConsumer2<Boolean>("AbstractTreeUi.doUpdateChildren") {
              @Override
              public void perform() {
                if (processAlwaysLeaf(node) || !updateChildren) return;
                updateNodeChildrenNow(node, pass, preloaded.get(), toSmartExpand, wasExpanded, childForceUpdate);
              }
            });
        }
        else {
          if (processAlwaysLeaf(node) || !updateChildren) return;

          updateNodeChildrenNow(node, pass, preloaded.get(), toSmartExpand, wasExpanded, childForceUpdate);
        }
      }
    }
    finally {
      if (!isReleased()) {
        processNodeActionsIfReady(node);
      }
    }
  }

  private boolean processAlwaysLeaf(@NotNull DefaultMutableTreeNode node) {
    Object element = getElementFor(node);
    NodeDescriptor2 desc = getDescriptorFrom(node);

    if (desc == null) return false;

    if (element != null && getTreeStructure().isAlwaysLeaf(element)) {
      removeFromUnbuilt(node);
      removeLoading(node, true);

      if (node.getChildCount() > 0) {
        final TreeNode[] children = new TreeNode[node.getChildCount()];
        for (int i = 0; i < node.getChildCount(); i++) {
          children[i] = node.getChildAt(i);
        }

        if (isSelectionInside(node)) {
          addSelectionPath(getPathFor(node), true, Conditions.alwaysTrue(), null);
        }

        processInnerChange(new TreeRunnable2("AbstractTreeUi.processAlwaysLeaf") {
          @Override
          public void perform() {
            for (TreeNode each : children) {
              removeNodeFromParent((MutableTreeNode)each, true);
              disposeNode((DefaultMutableTreeNode)each);
            }
          }
        });
      }

      removeFromUnbuilt(node);
      desc.setWasDeclaredAlwaysLeaf(true);
      processNodeActionsIfReady(node);
      return true;
    }
    else {
      boolean wasLeaf = desc.isWasDeclaredAlwaysLeaf();
      desc.setWasDeclaredAlwaysLeaf(false);

      if (wasLeaf) {
        insertLoadingNode(node, true);
      }

      return false;
    }
  }

  private boolean isChildNodeForceUpdate(@NotNull DefaultMutableTreeNode node, boolean parentForceUpdate, boolean parentExpanded) {
    TreePath path = getPathFor(node);
    return parentForceUpdate && (parentExpanded || myTree.isExpanded(path));
  }

  private void updateNodeChildrenNow(@NotNull final DefaultMutableTreeNode node,
                                     @NotNull final TreeUpdatePass2 pass,
                                     @Nullable final LoadedChildren preloadedChildren,
                                     final boolean toSmartExpand,
                                     final boolean wasExpanded,
                                     final boolean forceUpdate) {
    if (isUpdatingChildrenNow(node)) return;

    if (!canInitiateNewActivity()) {
      throw new ProcessCanceledException();
    }

    final NodeDescriptor2 descriptor = getDescriptorFrom(node);

    final MutualMap<Object, Integer> elementToIndexMap = loadElementsFromStructure(descriptor, preloadedChildren);
    final LoadedChildren loadedChildren =
      preloadedChildren != null ? preloadedChildren : new LoadedChildren(elementToIndexMap.getKeys().toArray());


    addToUpdatingChildren(node);
    pass.setCurrentNode(node);

    final boolean canSmartExpand = canSmartExpand(node, toSmartExpand);

    removeFromUnbuilt(node);

    //noinspection unchecked
    processExistingNodes(node, elementToIndexMap, pass, canSmartExpand(node, toSmartExpand), forceUpdate, wasExpanded, preloadedChildren)
      .onSuccess(new TreeConsumer2("AbstractTreeUi.updateNodeChildrenNow: on done processExistingNodes") {
        @Override
        public void perform() {
          if (isDisposed(node)) {
            removeFromUpdatingChildren(node);
            return;
          }

          removeLoading(node, false);

          final boolean expanded = isExpanded(node, wasExpanded);

          if (expanded) {
            myWillBeExpanded.add(node);
          }
          else {
            myWillBeExpanded.remove(node);
          }

          collectNodesToInsert(descriptor, elementToIndexMap, node, expanded, loadedChildren)
            .doWhenDone((Consumer<List<TreeNode>>)nodesToInsert -> {
              insertNodesInto(nodesToInsert, node);
              ActionCallback callback = updateNodesToInsert(nodesToInsert, pass, canSmartExpand, isChildNodeForceUpdate(node, forceUpdate, expanded));
              callback.doWhenDone(new TreeRunnable2("AbstractTreeUi.updateNodeChildrenNow: on done updateNodesToInsert") {
                @Override
                public void perform() {
                  removeLoading(node, false);
                  removeFromUpdatingChildren(node);

                  if (node.getChildCount() > 0) {
                    if (expanded) {
                      expand(node, canSmartExpand);
                    }
                  }

                  if (!canInitiateNewActivity()) {
                    throw new ProcessCanceledException();
                  }

                  final Object element = getElementFor(node);
                  addNodeAction(element, false, node1 -> removeLoading(node1, false));

                  processNodeActionsIfReady(node);
                }
              });
            }).doWhenProcessed(new TreeRunnable2("AbstractTreeUi.updateNodeChildrenNow: on processed collectNodesToInsert") {
            @Override
            public void perform() {
              myWillBeExpanded.remove(node);
              removeFromUpdatingChildren(node);
              processNodeActionsIfReady(node);
            }
          });
        }
      })
      .onError(new TreeConsumer2<Throwable>("AbstractTreeUi.updateNodeChildrenNow: on reject processExistingNodes") {
        @Override
        public void perform() {
          removeFromUpdatingChildren(node);
          processNodeActionsIfReady(node);
        }
    });
  }

  private boolean isDisposed(@NotNull DefaultMutableTreeNode node) {
    return !node.isNodeAncestor((DefaultMutableTreeNode)myTree.getModel().getRoot());
  }

  private void expandSilently(TreePath path) {
    assertIsDispatchThread();

    try {
      mySilentExpand = path;
      getTree().expandPath(path);
    }
    finally {
      mySilentExpand = null;
    }
  }

  private void addSelectionSilently(TreePath path) {
    assertIsDispatchThread();

    try {
      mySilentSelect = path;
      getTree().getSelectionModel().addSelectionPath(path);
    }
    finally {
      mySilentSelect = null;
    }
  }

  private void expand(@NotNull DefaultMutableTreeNode node, boolean canSmartExpand) {
    expand(new TreePath(node.getPath()), canSmartExpand);
  }

  private void expand(@NotNull final TreePath path, boolean canSmartExpand) {
    final Object last = path.getLastPathComponent();
    boolean isLeaf = myTree.getModel().isLeaf(path.getLastPathComponent());
    final boolean isRoot = last == myTree.getModel().getRoot();
    final TreePath parent = path.getParentPath();
    if (isRoot && !myTree.isExpanded(path)) {
      if (myTree.isRootVisible() || myUnbuiltNodes.contains(last)) {
        insertLoadingNode((DefaultMutableTreeNode)last, false);
      }
      expandPath(path, canSmartExpand);
    }
    else if (myTree.isExpanded(path) ||
             isLeaf && parent != null && myTree.isExpanded(parent) && !myUnbuiltNodes.contains(last) && !isCancelled(last)) {
      if (last instanceof DefaultMutableTreeNode) {
        processNodeActionsIfReady((DefaultMutableTreeNode)last);
      }
    }
    else {
      if (isLeaf && (myUnbuiltNodes.contains(last) || isCancelled(last))) {
        insertLoadingNode((DefaultMutableTreeNode)last, true);
        expandPath(path, canSmartExpand);
      }
      else if (isLeaf && parent != null) {
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parent.getLastPathComponent();
        if (parentNode != null) {
          addToUnbuilt(parentNode);
        }
        expandPath(parent, canSmartExpand);
      }
      else {
        expandPath(path, canSmartExpand);
      }
    }
  }

  private void addToUnbuilt(DefaultMutableTreeNode node) {
    myUnbuiltNodes.add(node);
  }

  private void removeFromUnbuilt(DefaultMutableTreeNode node) {
    myUnbuiltNodes.remove(node);
  }

  private Pair<Boolean, LoadedChildren> processUnbuilt(@NotNull final DefaultMutableTreeNode node,
                                                       final AbstractTreeNod2 descriptor,
                                                       @NotNull final TreeUpdatePass2 pass,
                                                       final boolean isExpanded,
                                                       @Nullable final LoadedChildren loadedChildren) {
    final Ref<Pair<Boolean, LoadedChildren>> result = new Ref<>();

    execute(new TreeRunnable2("AbstractTreeUi.processUnbuilt") {
      @Override
      public void perform() {
        if (!isExpanded && getBuilder().isAlwaysShowPlus(descriptor)) {
          result.set(new Pair<>(true, null));
          return;
        }

        final Object element = getElementFor(node);
        if (element == null) {
          trace("null element for node " + node);
          result.set(new Pair<>(true, null));
          return;
        }

        addToUpdatingChildren(node);

        try {
          final LoadedChildren children = loadedChildren != null ? loadedChildren : new LoadedChildren(getChildrenFor(element));

          boolean processed;

          if (children.getElements().isEmpty()) {
            removeFromUnbuilt(node);
            removeLoading(node, true);
            processed = true;
          } else {
            processed = false;
          }

          removeFromUpdatingChildren(node);

          processNodeActionsIfReady(node);

          result.set(new Pair<>(processed, children));
        }
        finally {
          removeFromUpdatingChildren(node);
        }
      }
    });

    return result.get();
  }

  private boolean removeIfLoading(@NotNull TreeNode node) {
    if (isLoadingNode(node)) {
      moveSelectionToParentIfNeeded(node);
      removeNodeFromParent((MutableTreeNode)node, false);
      return true;
    }

    return false;
  }

  private void moveSelectionToParentIfNeeded(@NotNull TreeNode node) {
    TreePath path = getPathFor(node);
    if (myTree.getSelectionModel().isPathSelected(path)) {
      TreePath parentPath = path.getParentPath();
      myTree.getSelectionModel().removeSelectionPath(path);
      if (parentPath != null) {
        myTree.getSelectionModel().addSelectionPath(parentPath);
      }
    }
  }

  //todo [kirillk] temporary consistency check
  private Object[] getChildrenFor(final Object element) {
    final Ref<Object[]> passOne = new Ref<>();
    try (LockToken ignored = acquireLock()) {
      execute(new TreeRunnable2("AbstractTreeUi.getChildrenFor") {
        @Override
        public void perform() {
          passOne.set(getTreeStructure().getChildElements(element));
        }
      });
    }
    catch (IndexNotReadyException e) {
      warnOnIndexNotReady(e);
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }

    if (!Registry.is("ide.tree.checkStructure")) return passOne.get();

    final Object[] passTwo = getTreeStructure().getChildElements(element);

    Set<Object> two = ContainerUtil.set(passTwo);

    if (passOne.get().length != passTwo.length) {
      LOG.error(
        "AbstractTreeStructure.getChildren() must either provide same objects or new objects but with correct hashCode() and equals() methods. Wrong parent element=" +
        element);
    }
    else {
      for (Object eachInOne : passOne.get()) {
        if (!two.contains(eachInOne)) {
          LOG.error(
            "AbstractTreeStructure.getChildren() must either provide same objects or new objects but with correct hashCode() and equals() methods. Wrong parent element=" +
            element);
          break;
        }
      }
    }

    return passOne.get();
  }

  private void warnOnIndexNotReady(IndexNotReadyException e) {
    if (!myWasEverIndexNotReady) {
      myWasEverIndexNotReady = true;
      LOG.error("Tree is not dumb-mode-aware; treeBuilder=" + getBuilder() + " treeStructure=" + getTreeStructure(), e);
    }
  }

  @NotNull
  private ActionCallback updateNodesToInsert(@NotNull final List<? extends TreeNode> nodesToInsert,
                                             @NotNull TreeUpdatePass2 pass,
                                             boolean canSmartExpand,
                                             boolean forceUpdate) {
    ActionCallback.Chunk chunk = new ActionCallback.Chunk();
    for (TreeNode node : nodesToInsert) {
      DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node;
      ActionCallback callback = updateNodeChildren(childNode, pass, null, false, canSmartExpand, forceUpdate, true, true);
      if (!callback.isDone()) {
        chunk.add(callback);
      }
    }
    return chunk.getWhenProcessed();
  }

  @NotNull
  private Promise<?> processExistingNodes(@NotNull final DefaultMutableTreeNode node,
                                             @NotNull final MutualMap<Object, Integer> elementToIndexMap,
                                             @NotNull final TreeUpdatePass2 pass,
                                             final boolean canSmartExpand,
                                             final boolean forceUpdate,
                                             final boolean wasExpanded,
                                             @Nullable final LoadedChildren preloaded) {
    final List<TreeNode> childNodes = TreeUtil.listChildren(node);
    return maybeYield(() -> {
      if (pass.isExpired()) return Promises.<Void>rejectedPromise();
      if (childNodes.isEmpty()) return Promises.resolvedPromise();


      List<Promise<?>> promises = new SmartList<>();
      for (TreeNode each : childNodes) {
        final DefaultMutableTreeNode eachChild = (DefaultMutableTreeNode)each;
        if (isLoadingNode(eachChild)) {
          continue;
        }

        final boolean childForceUpdate = isChildNodeForceUpdate(eachChild, forceUpdate, wasExpanded);

        promises.add(maybeYield(() -> {
          NodeDescriptor2 descriptor = preloaded != null ? preloaded.getDescriptor(getElementFor(eachChild)) : null;
          NodeDescriptor2 descriptorFromNode = getDescriptorFrom(eachChild);
          if (isValid(descriptor)) {
            eachChild.setUserObject(descriptor);
            if (descriptorFromNode != null) {
              descriptor.setChildrenSortingStamp(descriptorFromNode.getChildrenSortingStamp());
            }
          }
          else {
            descriptor = descriptorFromNode;
          }

          return processExistingNode(eachChild, descriptor, node, elementToIndexMap, pass, canSmartExpand,
                                     childForceUpdate, preloaded);
        }, pass, node));

        for (Promise<?> promise : promises) {
          if (promise.getState() == Promise.State.REJECTED) {
            return Promises.<Void>rejectedPromise();
          }
        }
      }
      return Promises.all(promises);
    }, pass, node);
  }

  private boolean isRerunNeeded(@NotNull TreeUpdatePass2 pass) {
    if (pass.isExpired() || !canInitiateNewActivity()) return false;

    final boolean rerunBecauseTreeIsHidden = !pass.isExpired() && !isTreeShowing() && getUpdater().isInPostponeMode();

    return rerunBecauseTreeIsHidden || getUpdater().isRerunNeededFor(pass);
  }

  @FunctionalInterface
  private interface AsyncRunnable {
    @NotNull
    Promise<?> run();
  }

  @NotNull
  private Promise<?> maybeYield(@NotNull final AsyncRunnable processRunnable, @NotNull final TreeUpdatePass2 pass, final DefaultMutableTreeNode node) {
    if (isRerunNeeded(pass)) {
      getUpdater().requeue(pass);
      return Promises.<Void>rejectedPromise();
    }

    if (canYield()) {
      final AsyncPromise<?> result = new AsyncPromise<Void>();
      pass.setCurrentNode(node);
      boolean wasRun = yieldAndRun(new TreeRunnable2("AbstractTreeUi.maybeYeild") {
        @Override
        public void perform() {
          if (pass.isExpired()) {
            result.setError("expired");
            return;
          }

          if (isRerunNeeded(pass)) {
            runDone(new TreeRunnable2("AbstractTreeUi.maybeYeild: rerun") {
              @Override
              public void perform() {
                if (!pass.isExpired()) {
                  queueUpdate(getElementFor(node));
                }
              }
            });
            result.setError("requeue");
          }
          else {
            try {
              //noinspection unchecked
              execute(processRunnable).processed((Promise)result);
            }
            catch (ProcessCanceledException e) {
              pass.expire();
              cancelUpdate();
              result.setError("rejected");
            }
          }
        }
      }, pass);
      if (!wasRun) {
        result.setError("rejected");
      }
      return result;
    }
    else {
      try {
        return execute(processRunnable);
      }
      catch (ProcessCanceledException e) {
        pass.expire();
        cancelUpdate();
        return Promises.<Void>rejectedPromise();
      }
    }
  }

  @NotNull
  private Promise<?> execute(@NotNull AsyncRunnable runnable) throws ProcessCanceledException {
    try {
      if (!canInitiateNewActivity()) {
        throw new ProcessCanceledException();
      }

      Promise<?> promise = runnable.run();
      if (!canInitiateNewActivity()) {
        throw new ProcessCanceledException();
      }
      return promise;
    }
    catch (ProcessCanceledException e) {
      if (!isReleased()) {
        setCancelRequested(true);
        resetToReady();
      }
      throw e;
    }
  }

  private void execute(@NotNull Runnable runnable) throws ProcessCanceledException {
    try {
      if (!canInitiateNewActivity()) {
        throw new ProcessCanceledException();
      }

      runnable.run();

      if (!canInitiateNewActivity()) {
        throw new ProcessCanceledException();
      }
    }
    catch (ProcessCanceledException e) {
      if (!isReleased()) {
        setCancelRequested(true);
        resetToReady();
      }
      throw e;
    }
  }

  private boolean canInitiateNewActivity() {
    return !isCancelProcessed() && !myReleaseRequested && !isReleased();
  }

  private void resetToReady() {
    if (isReady()) {
      return;
    }

    if (myResettingToReadyNow.get()) {
      _getReady();
      return;
    }

    myResettingToReadyNow.set(true);

    invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.resetToReady: later") {
      @Override
      public void perform() {
        if (!myResettingToReadyNow.get()) {
          return;
        }

        Progressive[] progressives = myBatchIndicators.keySet().toArray(new Progressive[0]);
        for (Progressive each : progressives) {
          myBatchIndicators.remove(each).cancel();
          myBatchCallbacks.remove(each).setRejected();
        }

        resetToReadyNow();
      }
    });
  }

  @NotNull
  private ActionCallback resetToReadyNow() {
    if (isReleased()) return ActionCallback.REJECTED;

    assertIsDispatchThread();

    DefaultMutableTreeNode[] uc;
    synchronized (myUpdatingChildren) {
      uc = myUpdatingChildren.toArray(new DefaultMutableTreeNode[0]);
    }
    for (DefaultMutableTreeNode each : uc) {
      resetIncompleteNode(each);
    }


    Object[] bg = ArrayUtil.toObjectArray(myLoadedInBackground.keySet());
    for (Object each : bg) {
      final DefaultMutableTreeNode node = getNodeForElement(each, false);
      if (node != null) {
        resetIncompleteNode(node);
      }
    }

    myUpdaterState = null;
    getUpdater().reset();

    myYieldingNow = false;
    myYieldingPasses.clear();
    myYieldingDoneRunnables.clear();

    myNodeActions.clear();
    myNodeChildrenActions.clear();

    synchronized (myUpdatingChildren) {
      myUpdatingChildren.clear();
    }
    myLoadedInBackground.clear();

    myDeferredExpansions.clear();
    myDeferredSelections.clear();

    ActionCallback result = _getReady();
    result.doWhenDone(new TreeRunnable2("AbstractTreeUi.resetToReadyNow: on done") {
      @Override
      public void perform() {
        myResettingToReadyNow.set(false);
        setCancelRequested(false);
      }
    });

    maybeReady();

    return result;
  }

  void addToCancelled(@NotNull DefaultMutableTreeNode node) {
    myCancelledBuild.put(node, node);
  }

  private void removeFromCancelled(@NotNull DefaultMutableTreeNode node) {
    myCancelledBuild.remove(node);
  }

  public boolean isCancelled(@NotNull Object node) {
    return node instanceof DefaultMutableTreeNode && myCancelledBuild.containsKey(node);
  }

  private void resetIncompleteNode(@NotNull DefaultMutableTreeNode node) {
    if (myReleaseRequested) return;

    addToCancelled(node);

    if (!isExpanded(node, false)) {
      node.removeAllChildren();
      Object element = getElementFor(node);
      if (element != null && !getTreeStructure().isAlwaysLeaf(element)) {
        insertLoadingNode(node, true);
      }
    }
    else {
      removeFromUnbuilt(node);
      removeLoading(node, true);
    }
  }

  private boolean yieldAndRun(@NotNull final Runnable runnable, @NotNull final TreeUpdatePass2 pass) {
    myYieldingPasses.add(pass);
    myYieldingNow = true;
    yieldToEDT(new TreeRunnable2("AbstractTreeUi.yieldAndRun") {
      @Override
      public void perform() {
        if (isReleased()) return;

        runOnYieldingDone(new TreeRunnable2("AbstractTreeUi.yieldAndRun: inner") {
          @Override
          public void perform() {
            if (isReleased()) return;

            executeYieldingRequest(runnable, pass);
          }
        });
      }
    });

    return true;
  }

  private boolean isYeildingNow() {
    return myYieldingNow;
  }

  private boolean hasScheduledUpdates() {
    return getUpdater().hasNodesToUpdate();
  }

  public boolean isReady() {
    return isReady(false);
  }

  public boolean isReady(boolean attempt) {
    if (attempt && myStateLock.isLocked()) return false;

    Boolean ready = checkValue(() -> isIdle() && !hasPendingWork() && !isNodeActionsPending(), attempt);

    return ready != null && ready.booleanValue();
  }

  @Nullable
  private Boolean checkValue(@NotNull Computable<Boolean> computable, boolean attempt) {
    try (LockToken ignored = attempt ? attemptLock() : acquireLock()) {
      return computable.compute();
    }
    catch (InterruptedException e) {
      LOG.info(e);
      return null;
    }
  }

  public boolean hasPendingWork() {
    return hasNodesToUpdate() ||
           myUpdaterState != null && myUpdaterState.isProcessingNow() ||
           hasScheduledUpdates() && !getUpdater().isInPostponeMode();
  }

  public boolean isIdle() {
    return !isYeildingNow() && !isWorkerBusy() && !hasUpdatingChildrenNow() && !isLoadingInBackgroundNow();
  }

  private void executeYieldingRequest(@NotNull Runnable runnable, @NotNull TreeUpdatePass2 pass) {
    try {
      try {
        myYieldingPasses.remove(pass);

        if (!canInitiateNewActivity()) {
          throw new ProcessCanceledException();
        }

        runnable.run();
      }
      finally {
        if (!isReleased()) {
          maybeYieldingFinished();
        }
      }
    }
    catch (ProcessCanceledException e) {
      resetToReady();
    }
  }

  private void maybeYieldingFinished() {
    if (myYieldingPasses.isEmpty()) {
      myYieldingNow = false;
      flushPendingNodeActions();
    }
  }

  void maybeReady() {
    assertIsDispatchThread();

    if (isReleased()) return;

    boolean ready = isReady(true);
    if (!ready) return;
    myRevalidatedObjects.clear();

    setCancelRequested(false);
    myResettingToReadyNow.set(false);

    myInitialized.setDone();

    if (canInitiateNewActivity()) {
      if (myUpdaterState != null && !myUpdaterState.isProcessingNow()) {
        UpdaterTreeState2 oldState = myUpdaterState;
        if (!myUpdaterState.restore(null)) {
          setUpdaterState(oldState);
        }

        if (!isReady(true)) return;
      }
    }

    setHoldSize(false);

    if (myTree.isShowing()) {
      if (Uni.fileManagerConf2.ensureSelectionOnFocusGained) {
        TreeUtil.ensureSelection(myTree);
      }
    }

    if (myInitialized.isDone()) {
      if (isReleaseRequested() || isCancelProcessed()) {
        myBusyObject.onReady(this);
      } else {
        myBusyObject.onReady();
      }
    }

    if (canInitiateNewActivity()) {
      TreePath[] selection = getTree().getSelectionPaths();
      Rectangle visible = getTree().getVisibleRect();
      if (selection != null) {
        for (TreePath each : selection) {
          Rectangle bounds = getTree().getPathBounds(each);
          if (bounds != null && (visible.contains(bounds) || visible.intersects(bounds))) {
            getTree().repaint(bounds);
          }
        }
      }
    }
  }

  private void flushPendingNodeActions() {
    final DefaultMutableTreeNode[] nodes = myPendingNodeActions.toArray(new DefaultMutableTreeNode[0]);
    myPendingNodeActions.clear();

    for (DefaultMutableTreeNode each : nodes) {
      processNodeActionsIfReady(each);
    }

    final Runnable[] actions = myYieldingDoneRunnables.toArray(new Runnable[0]);
    for (Runnable each : actions) {
      if (!isYeildingNow()) {
        myYieldingDoneRunnables.remove(each);
        each.run();
      }
    }

    maybeReady();
  }

  protected void runOnYieldingDone(@NotNull Runnable onDone) {
    getBuilder().runOnYieldingDone(onDone);
  }

  protected void yieldToEDT(@NotNull Runnable runnable) {
    getBuilder().yieldToEDT(runnable);
  }

  @NotNull
  private MutualMap<Object, Integer> loadElementsFromStructure(final NodeDescriptor2 descriptor,
                                                               @Nullable LoadedChildren preloadedChildren) {
    MutualMap<Object, Integer> elementToIndexMap = new MutualMap<>(true);
    final Object element = getElementFromDescriptor(descriptor);
    if (!isValid(element)) return elementToIndexMap;


    List<Object> children = preloadedChildren != null
                            ? preloadedChildren.getElements()
                            : Arrays.asList(getChildrenFor(element));
    int index = 0;
    for (Object child : children) {
      if (!isValid(child)) continue;
      elementToIndexMap.put(child, index);
      index++;
    }
    return elementToIndexMap;
  }

  public static boolean isLoadingNode(final Object node) {
    return node instanceof LoadingNode;
  }

  @NotNull
  private AsyncResult<List<TreeNode>> collectNodesToInsert(final NodeDescriptor2 descriptor,
                                                           @NotNull final MutualMap<Object, Integer> elementToIndexMap,
                                                           final DefaultMutableTreeNode parent,
                                                           final boolean addLoadingNode,
                                                           @NotNull final LoadedChildren loadedChildren) {
    final AsyncResult<List<TreeNode>> result = new AsyncResult<>();

    final List<TreeNode> nodesToInsert = new ArrayList<>();
    Collection<Object> allElements = elementToIndexMap.getKeys();

    ActionCallback processingDone = allElements.isEmpty() ? ActionCallback.DONE : new ActionCallback(allElements.size());

    for (final Object child : allElements) {
      Integer index = elementToIndexMap.getValue(child);
      boolean needToUpdate = false;
      AbstractTreeNod2 loadedDesc = loadedChildren.getDescriptor(child);
      final AbstractTreeNod2 childDescr;
      if (!isValid(loadedDesc, descriptor)) {
        childDescr = (AbstractTreeNod2<?>)child;
        needToUpdate = true;
      }
      else {
        childDescr = loadedDesc;
      }

      if (index == null) {
        index = Integer.MAX_VALUE;
        needToUpdate = true;
      }

      childDescr.setIndex(index.intValue());

      final ActionCallback update = new ActionCallback();
      if (needToUpdate) {
        update(childDescr, false)
          .onSuccess(changes -> {
            loadedChildren.putDescriptor(child, childDescr, changes);
            update.setDone();
          });
      }
      else {
        update.setDone();
      }

      update.doWhenDone(new TreeRunnable2("AbstractTreeUi.collectNodesToInsert: on done update") {
        @Override
        public void perform() {
          Object element = getElementFromDescriptor(childDescr);
          if (!isNodeNull(element)) {
            DefaultMutableTreeNode node = getNodeForElement(element, false);
            if (node == null || node.getParent() != parent) {
              final DefaultMutableTreeNode childNode = createChildNode(childDescr);
              if (addLoadingNode || getBuilder().isAlwaysShowPlus(childDescr)) {
                insertLoadingNode(childNode, true);
              }
              else {
                addToUnbuilt(childNode);
              }
              nodesToInsert.add(childNode);
              createMapping(element, childNode);
            }
          }
          processingDone.setDone();
        }
      });
    }

    processingDone.doWhenDone(new TreeRunnable2("AbstractTreeUi.collectNodesToInsert: on done processing") {
      @Override
      public void perform() {
        result.setDone(nodesToInsert);
      }
    });

    return result;
  }

  @NotNull
  protected DefaultMutableTreeNode createChildNode(final NodeDescriptor2 descriptor) {
    return new ElementNode(this, descriptor);
  }

  protected boolean canYield() {
    return myCanYield && myYieldingUpdate.asBoolean();
  }

  private long getClearOnHideDelay() {
    return myClearOnHideDelay;
  }

  public ActionCallback getReady(@NotNull Object requestor) {
    return myBusyObject.getReady(requestor);
  }

  private ActionCallback _getReady() {
    return getReady(this);
  }

  private void addToUpdatingChildren(@NotNull DefaultMutableTreeNode node) {
    synchronized (myUpdatingChildren) {
      myUpdatingChildren.add(node);
    }
  }

  private void removeFromUpdatingChildren(@NotNull DefaultMutableTreeNode node) {
    synchronized (myUpdatingChildren) {
      myUpdatingChildren.remove(node);
    }
  }

  boolean isUpdatingChildrenNow(DefaultMutableTreeNode node) {
    synchronized (myUpdatingChildren) {
      return myUpdatingChildren.contains(node);
    }
  }

  boolean isParentUpdatingChildrenNow(@NotNull DefaultMutableTreeNode node) {
    synchronized (myUpdatingChildren) {
      DefaultMutableTreeNode eachParent = (DefaultMutableTreeNode)node.getParent();
      while (eachParent != null) {
        if (myUpdatingChildren.contains(eachParent)) return true;

        eachParent = (DefaultMutableTreeNode)eachParent.getParent();
      }

      return false;
    }
  }

  private boolean hasUpdatingChildrenNow() {
    synchronized (myUpdatingChildren) {
      return !myUpdatingChildren.isEmpty();
    }
  }

  @NotNull
  List<Object> getLoadedChildrenFor(@NotNull Object element) {
    List<Object> result = new ArrayList<>();

    DefaultMutableTreeNode node = getNodeForElement(element, false);
    if (node != null) {
      for (int i = 0; i < node.getChildCount(); i++) {
        TreeNode each = node.getChildAt(i);
        if (isLoadingNode(each)) continue;

        result.add(getElementFor(each));
      }
    }

    return result;
  }

  boolean hasNodesToUpdate() {
    return getUpdater().hasNodesToUpdate();
  }

  @NotNull
  public ActionCallback cancelUpdate() {
    if (isReleased()) return ActionCallback.REJECTED;

    setCancelRequested(true);

    final ActionCallback done = new ActionCallback();

    invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.cancelUpdate") {
      @Override
      public void perform() {
        if (isReleased()) {
          done.setRejected();
          return;
        }

        if (myResettingToReadyNow.get()) {
          _getReady().notify(done);
        } else if (isReady()) {
          resetToReadyNow();
          done.setDone();
        } else {
          if (isIdle() && hasPendingWork()) {
            resetToReadyNow();
            done.setDone();
          } else {
            _getReady().notify(done);
          }
        }

        maybeReady();
      }
    });

    if (isEdt() || isPassthroughMode()) {
      maybeReady();
    }

    return done;
  }

  private void setCancelRequested(boolean requested) {
    try (LockToken ignored = isUnitTestingMode() ? acquireLock() : attemptLock()) {
      myCancelRequest.set(requested);
    }
    catch (InterruptedException ignored) {
    }
  }

  @Nullable
  private LockToken attemptLock() throws InterruptedException {
    return LockToken.attemptLock(myStateLock, Registry.intValue("ide.tree.uiLockAttempt"));
  }

  @NotNull
  private LockToken acquireLock() {
    return LockToken.acquireLock(myStateLock);
  }

  public void batch(@NotNull final Progressive progressive) {
    assertIsDispatchThread();

    EmptyProgressIndicator indicator = new EmptyProgressIndicator();
    final ActionCallback callback = new ActionCallback();

    myBatchIndicators.put(progressive, indicator);
    myBatchCallbacks.put(progressive, callback);

    try {
      progressive.run(indicator);
    }
    catch (ProcessCanceledException e) {
      resetToReadyNow().doWhenProcessed(new TreeRunnable2("AbstractTreeUi.batch: catch") {
        @Override
        public void perform() {
          callback.setRejected();
        }
      });
    }
    finally {
      if (isReleased()) return;

      _getReady().doWhenDone(new TreeRunnable2("AbstractTreeUi.batch: finally") {
        @Override
        public void perform() {
          if (myBatchIndicators.containsKey(progressive)) {
            ProgressIndicator indicator = myBatchIndicators.remove(progressive);
            myBatchCallbacks.remove(progressive);

            if (indicator.isCanceled()) {
              callback.setRejected();
            } else {
              callback.setDone();
            }
          } else {
            callback.setRejected();
          }
        }
      });

      maybeReady();
    }


  }

  boolean isCancelProcessed() {
    return myCancelRequest.get() || myResettingToReadyNow.get();
  }

  boolean isToPaintSelection() {
    return isReady(true) || !mySelectionIsAdjusted;
  }

  boolean isReleaseRequested() {
    return myReleaseRequested;
  }

  public void executeUserRunnable(@NotNull Runnable runnable) {
    try {
      myUserRunnables.add(runnable);
      runnable.run();
    }
    finally {
      myUserRunnables.remove(runnable);
    }
  }

  static class ElementNode extends DefaultMutableTreeNode {

    final Set<Object> myElements = new HashSet<>();
    final AbstractTreeUi2 myUi;

    ElementNode(AbstractTreeUi2 ui, NodeDescriptor2 descriptor) {
      super(descriptor);
      myUi = ui;
    }

    @Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
      super.insert(newChild, childIndex);
      final Object element = myUi.getElementFor(newChild);
      if (element != null) {
        myElements.add(element);
      }
    }

    @Override
    public void remove(final int childIndex) {
      final TreeNode node = getChildAt(childIndex);
      super.remove(childIndex);
      final Object element = myUi.getElementFor(node);
      if (element != null) {
        myElements.remove(element);
      }
    }

    boolean isValidChild(Object childElement) {
      return myElements.contains(childElement);
    }

    @Override
    public String toString() {
      return String.valueOf(getUserObject());
    }
  }

  private boolean isUpdatingParent(DefaultMutableTreeNode kid) {
    return getUpdatingParent(kid) != null;
  }

  @Nullable
  private DefaultMutableTreeNode getUpdatingParent(DefaultMutableTreeNode kid) {
    DefaultMutableTreeNode eachParent = kid;
    while (eachParent != null) {
      if (isUpdatingChildrenNow(eachParent)) return eachParent;
      eachParent = (DefaultMutableTreeNode)eachParent.getParent();
    }

    return null;
  }

  private boolean isLoadedInBackground(Object element) {
    return getLoadedInBackground(element) != null;
  }

  private UpdateInfo getLoadedInBackground(Object element) {
    synchronized (myLoadedInBackground) {
      return isNodeNull(element) ? null : myLoadedInBackground.get(element);
    }
  }

  private void addToLoadedInBackground(Object element, UpdateInfo info) {
    if (isNodeNull(element)) return;
    synchronized (myLoadedInBackground) {
      warnMap("put into myLoadedInBackground: ", myLoadedInBackground);
      myLoadedInBackground.put(element, info);
    }
  }

  private void removeFromLoadedInBackground(final Object element) {
    if (isNodeNull(element)) return;
    synchronized (myLoadedInBackground) {
      warnMap("remove from myLoadedInBackground: ", myLoadedInBackground);
      myLoadedInBackground.remove(element);
    }
  }

  private boolean isLoadingInBackgroundNow() {
    synchronized (myLoadedInBackground) {
      return !myLoadedInBackground.isEmpty();
    }
  }

  private void queueBackgroundUpdate(@NotNull final UpdateInfo updateInfo, @NotNull final DefaultMutableTreeNode node) {
    assertIsDispatchThread();

    final Object oldElementFromDescriptor = getElementFromDescriptor(updateInfo.getDescriptor());
    if (isNodeNull(oldElementFromDescriptor)) return;

    UpdateInfo loaded = getLoadedInBackground(oldElementFromDescriptor);
    if (loaded != null) {
      loaded.apply(updateInfo);
      return;
    }

    addToLoadedInBackground(oldElementFromDescriptor, updateInfo);

    maybeSetBusyAndScheduleWaiterForReady(true, oldElementFromDescriptor);

    if (!isNodeBeingBuilt(node)) {
      LoadingNode loadingNode = new LoadingNode(getLoadingNodeText());
      myTreeModel.insertNodeInto(loadingNode, node, node.getChildCount());
    }

    removeFromUnbuilt(node);

    final Ref<LoadedChildren> children = new Ref<>();
    final Ref<Object> elementFromDescriptor = new Ref<>();

    final DefaultMutableTreeNode[] nodeToProcessActions = new DefaultMutableTreeNode[1];

    final TreeConsumer2<Void> finalizeRunnable = new TreeConsumer2<Void>("AbstractTreeUi.queueBackgroundUpdate: finalize") {
      @Override
      public void perform() {
        invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.queueBackgroundUpdate: finalize later") {
          @Override
          public void perform() {
            if (isReleased()) return;

            removeLoading(node, false);
            removeFromLoadedInBackground(elementFromDescriptor.get());
            removeFromLoadedInBackground(oldElementFromDescriptor);

            if (nodeToProcessActions[0] != null) {
              processNodeActionsIfReady(nodeToProcessActions[0]);
            }
          }
        });
      }
    };


    Runnable buildRunnable = new TreeRunnable2("AbstractTreeUi.queueBackgroundUpdate: build") {
      @Override
      public void perform() {
        if (updateInfo.getPass().isExpired()) {
          finalizeRunnable.run();
          return;
        }

        if (!updateInfo.isDescriptorIsUpToDate()) {
          update(updateInfo.getDescriptor(), true);
        }

        if (!updateInfo.isUpdateChildren()) {
          nodeToProcessActions[0] = node;
          return;
        }

        Object element = getElementFromDescriptor(updateInfo.getDescriptor());
        if (element == null) {
          removeFromLoadedInBackground(oldElementFromDescriptor);
          finalizeRunnable.run();
          return;
        }

        elementFromDescriptor.set(element);

        Object[] loadedElements = getChildrenFor(element);

        final LoadedChildren loaded = new LoadedChildren(loadedElements);
        for (final Object each : loadedElements) {
          AbstractTreeNod2<?> existingDesc = getDescriptorFrom(getNodeForElement(each, true));
          AbstractTreeNod2<?> eachChildDescriptor = isValid(existingDesc, updateInfo.getDescriptor()) ? existingDesc : (AbstractTreeNod2<?>) each;
          execute(new TreeRunnable2("AbstractTreeUi.queueBackgroundUpdate") {
            @Override
            public void perform() {
              try {
                loaded.putDescriptor(each, eachChildDescriptor, update(eachChildDescriptor, true).blockingGet(0));
              }
              catch (TimeoutException | ExecutionException e) {
                LOG.error(e);
              }
            }
          });
        }

        children.set(loaded);
      }

      @NotNull
      @NonNls
      @Override
      public String toString() {
        return "runnable=" + oldElementFromDescriptor;
      }
    };

    Runnable updateRunnable = new TreeRunnable2("AbstractTreeUi.queueBackgroundUpdate: update") {
      @Override
      public void perform() {
        if (updateInfo.getPass().isExpired()) {
          finalizeRunnable.run();
          return;
        }

        if (children.get() == null) {
          finalizeRunnable.run();
          return;
        }

        if (isRerunNeeded(updateInfo.getPass())) {
          removeFromLoadedInBackground(elementFromDescriptor.get());
          getUpdater().requeue(updateInfo.getPass());
          return;
        }

        removeFromLoadedInBackground(elementFromDescriptor.get());

        if (myUnbuiltNodes.contains(node)) {
          Pair<Boolean, LoadedChildren> unbuilt =
            processUnbuilt(node, updateInfo.getDescriptor(), updateInfo.getPass(), isExpanded(node, updateInfo.isWasExpanded()),
                           children.get());
          if (unbuilt.getFirst()) {
            nodeToProcessActions[0] = node;
            return;
          }
        }

        ActionCallback callback = updateNodeChildren(node, updateInfo.getPass(), children.get(),
                                                     true, updateInfo.isCanSmartExpand(), updateInfo.isForceUpdate(), true, true);
        callback.doWhenDone(new TreeRunnable2("AbstractTreeUi.queueBackgroundUpdate: on done updateNodeChildren") {
          @Override
          public void perform() {
            if (isRerunNeeded(updateInfo.getPass())) {
              getUpdater().requeue(updateInfo.getPass());
              return;
            }

            Object element = elementFromDescriptor.get();

            if (element != null) {
              removeLoading(node, false);
              nodeToProcessActions[0] = node;
            }
          }
        });
      }
    };
    queueToBackground(buildRunnable, updateRunnable)
      .onSuccess(finalizeRunnable)
      .onError(new TreeConsumer2<Throwable>("AbstractTreeUi.queueBackgroundUpdate: on rejected") {
        @Override
        public void perform() {
          updateInfo.getPass().expire();
        }
      });
  }

  private boolean isExpanded(@NotNull DefaultMutableTreeNode node, boolean isExpanded) {
    return isExpanded || myTree.isExpanded(getPathFor(node));
  }

  private void removeLoading(@NotNull DefaultMutableTreeNode parent, boolean forced) {
    if (!forced && myUnbuiltNodes.contains(parent) && !myCancelledBuild.containsKey(parent)) {
      return;
    }

    boolean reallyRemoved = false;
    for (int i = 0; i < parent.getChildCount(); i++) {
      TreeNode child = parent.getChildAt(i);
      if (removeIfLoading(child)) {
        reallyRemoved = true;
        i--;
      }
    }

    maybeReady();
    if (reallyRemoved) {
      nodeStructureChanged(parent);
    }
  }

  private void processNodeActionsIfReady(@NotNull final DefaultMutableTreeNode node) {
    assertIsDispatchThread();

    if (isNodeBeingBuilt(node)) return;

    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;


    if (isYeildingNow()) {
      myPendingNodeActions.add(node);
      return;
    }

    final Object element = getElementFromDescriptor(descriptor);

    boolean childrenReady = !isLoadedInBackground(element) && !isUpdatingChildrenNow(node);

    processActions(node, element, myNodeActions, childrenReady ? myNodeChildrenActions : null);
    if (childrenReady) {
      processActions(node, element, myNodeChildrenActions, null);
    }
    warnMap("myNodeActions: processNodeActionsIfReady: ", myNodeActions);
    warnMap("myNodeChildrenActions: processNodeActionsIfReady: ", myNodeChildrenActions);

    if (!isUpdatingParent(node) && !isWorkerBusy()) {
      final UpdaterTreeState2 state = myUpdaterState;
      if (myNodeActions.isEmpty() && state != null && !state.isProcessingNow()) {
        if (canInitiateNewActivity()) {
          if (!state.restore(childrenReady ? node : null)) {
            setUpdaterState(state);
          }
        }
      }
    }

    maybeReady();
  }


  private static void processActions(@NotNull DefaultMutableTreeNode node,
                                     Object element,
                                     @NotNull final Map<Object, List<NodeAction>> nodeActions,
                                     @Nullable final Map<Object, List<NodeAction>> secondaryNodeAction) {
    final List<NodeAction> actions = nodeActions.get(element);
    if (actions != null) {
      nodeActions.remove(element);

      List<NodeAction> secondary = secondaryNodeAction != null ? secondaryNodeAction.get(element) : null;
      for (NodeAction each : actions) {
        if (secondary != null) {
          secondary.remove(each);
        }
        each.onReady(node);
      }
    }
  }

  private boolean canSmartExpand(DefaultMutableTreeNode node, boolean canSmartExpand) {
    if (!canInitiateNewActivity()) return false;

    boolean smartExpand = canSmartExpand && !myNotForSmartExpand.contains(node);
    Object element = getElementFor(node);
    return smartExpand && element != null;
  }

  private void processSmartExpand(@NotNull final DefaultMutableTreeNode node, final boolean canSmartExpand, boolean forced) {
    if (!canInitiateNewActivity()) return;

    boolean can = canSmartExpand(node, canSmartExpand);

    if (!can && !forced) return;

    if (isNodeBeingBuilt(node) && !forced) {
      addNodeAction(getElementFor(node), true, node1 -> processSmartExpand(node1, canSmartExpand, true));
    }
    else {
      TreeNode child = getChildForSmartExpand(node);
      if (child != null) {
        final TreePath childPath = new TreePath(node.getPath()).pathByAddingChild(child);
        processInnerChange(new TreeRunnable2("AbstractTreeUi.processSmartExpand") {
          @Override
          public void perform() {
            myTree.expandPath(childPath);
          }
        });
      }
    }
  }

  @Nullable
  private static TreeNode getChildForSmartExpand(@NotNull DefaultMutableTreeNode node) {
    int realChildCount = 0;
    TreeNode nodeToExpand = null;

    for (int i = 0; i < node.getChildCount(); i++) {
      TreeNode eachChild = node.getChildAt(i);

      if (!isLoadingNode(eachChild)) {
        realChildCount++;
        if (nodeToExpand == null) {
          nodeToExpand = eachChild;
        }
      }

      if (realChildCount > 1) {
        nodeToExpand = null;
        break;
      }
    }

    return nodeToExpand;
  }

  public static boolean isLoadingChildrenFor(final Object nodeObject) {
    if (!(nodeObject instanceof DefaultMutableTreeNode)) return false;

    DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodeObject;

    int loadingNodes = 0;
    for (int i = 0; i < Math.min(node.getChildCount(), 2); i++) {
      TreeNode child = node.getChildAt(i);
      if (isLoadingNode(child)) {
        loadingNodes++;
      }
    }
    return loadingNodes > 0 && loadingNodes == node.getChildCount();
  }

  boolean isParentLoadingInBackground(@NotNull Object nodeObject) {
    return getParentLoadingInBackground(nodeObject) != null;
  }

  @Nullable
  private DefaultMutableTreeNode getParentLoadingInBackground(@NotNull Object nodeObject) {
    if (!(nodeObject instanceof DefaultMutableTreeNode)) return null;

    DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodeObject;

    TreeNode eachParent = node.getParent();

    while (eachParent != null) {
      eachParent = eachParent.getParent();
      if (eachParent instanceof DefaultMutableTreeNode) {
        final Object eachElement = getElementFor(eachParent);
        if (isLoadedInBackground(eachElement)) return (DefaultMutableTreeNode)eachParent;
      }
    }

    return null;
  }

  private static @Nls String getLoadingNodeText() {
    return IdeBundle.message("progress.searching");
  }

  @NotNull
  private Promise<?> processExistingNode(@NotNull final DefaultMutableTreeNode childNode,
                                             final NodeDescriptor2 childDescriptor,
                                             @NotNull final DefaultMutableTreeNode parentNode,
                                             @NotNull final MutualMap<Object, Integer> elementToIndexMap,
                                             @NotNull final TreeUpdatePass2 pass,
                                             final boolean canSmartExpand,
                                             final boolean forceUpdate,
                                             @Nullable LoadedChildren parentPreloadedChildren) {
    if (pass.isExpired()) {
      return Promises.<Void>rejectedPromise();
    }

    if (childDescriptor == null) {
      pass.expire();
      return Promises.<Void>rejectedPromise();
    }
    final Object oldElement = getElementFromDescriptor(childDescriptor);
    if (isNodeNull(oldElement)) {
      // if a tree node with removed element was not properly removed from a tree model
      // we must not ignore this situation and should remove a wrong node
      removeNodeFromParent(childNode, true);
      doUpdateNode(parentNode);
      return Promises.<Void>resolvedPromise();
    }

    Promise<Boolean> update;
    if (parentPreloadedChildren != null && parentPreloadedChildren.getDescriptor(oldElement) == childDescriptor) {
      update = Promises.resolvedPromise(parentPreloadedChildren.isUpdated(oldElement));
    }
    else {
      update = update(childDescriptor, false);
    }

    final AsyncPromise<Void> result = new AsyncPromise<>();
    final Ref<NodeDescriptor2> childDesc = new Ref<>(childDescriptor);

    update
      .onSuccess(isChanged -> {
        final AtomicBoolean changes = new AtomicBoolean(isChanged);
        final AtomicBoolean forceRemapping = new AtomicBoolean();
        final Ref<Object> newElement = new Ref<>(getElementFromDescriptor(childDesc.get()));

        final Integer index = newElement.get() == null ? null : elementToIndexMap.getValue(getElementFromDescriptor(childDesc.get()));
        Promise<Boolean> promise;
        if (index == null) {
          promise = Promises.resolvedPromise(false);
        }
        else {
          final Object elementFromMap = elementToIndexMap.getKey(index);
          if (elementFromMap != newElement.get() && elementFromMap.equals(newElement.get())) {
            if (isInStructure(elementFromMap) && isInStructure(newElement.get())) {
              final AsyncPromise<Boolean> updateIndexDone = new AsyncPromise<>();
              promise = updateIndexDone;
              NodeDescriptor2 parentDescriptor = getDescriptorFrom(parentNode);
              if (parentDescriptor != null) {
                childDesc.set((NodeDescriptor2) elementFromMap);
                NodeDescriptor2 oldDesc = getDescriptorFrom(childNode);
                if (isValid(oldDesc)) {
                  childDesc.get().applyFrom(oldDesc);
                }

                childNode.setUserObject(childDesc.get());
                newElement.set(elementFromMap);
                forceRemapping.set(true);
                update(childDesc.get(), false)
                  .onSuccess(isChanged1 -> {
                    changes.set(isChanged1);
                    updateIndexDone.setResult(isChanged1);
                  });
              }
              // todo why we don't process promise here?
            }
            else {
              promise = Promises.resolvedPromise(changes.get());
            }
          }
          else {
            promise = Promises.resolvedPromise(changes.get());
          }

          promise
            .onSuccess(new TreeConsumer2<Boolean>("AbstractTreeUi.processExistingNode: on done index updating after update") {
              @Override
              public void perform() {
                if (childDesc.get().getIndex() != index.intValue()) {
                  changes.set(true);
                }
                childDesc.get().setIndex(index.intValue());
              }
            });
        }

        promise
          .onSuccess(new TreeConsumer2<Boolean>("AbstractTreeUi.processExistingNode: on done index updating") {
            @Override
            public void perform() {
              if (!oldElement.equals(newElement.get()) || forceRemapping.get()) {
                removeMapping(oldElement, childNode, newElement.get());
                Object newE = newElement.get();
                if (!isNodeNull(newE)) {
                  createMapping(newE, childNode);
                }
                NodeDescriptor2 parentDescriptor = getDescriptorFrom(parentNode);
                if (parentDescriptor != null) {
                  parentDescriptor.setChildrenSortingStamp(-1);
                }
              }

              if (index == null) {
                int selectedIndex = -1;
                if (TreeBuilderUtil2.isNodeOrChildSelected(myTree, childNode)) {
                  selectedIndex = parentNode.getIndex(childNode);
                }

                if (childNode.getParent() instanceof DefaultMutableTreeNode) {
                  final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)childNode.getParent();
                  if (myTree.isExpanded(new TreePath(parent.getPath()))) {
                    if (parent.getChildCount() == 1 && parent.getChildAt(0) == childNode) {
                      insertLoadingNode(parent, false);
                    }
                  }
                }

                Object disposedElement = getElementFor(childNode);

                removeNodeFromParent(childNode, selectedIndex >= 0);
                disposeNode(childNode);

                adjustSelectionOnChildRemove(parentNode, selectedIndex, disposedElement);
                result.setResult(null);
              }
              else {
                elementToIndexMap.remove(getElementFromDescriptor(childDesc.get()));
                updateNodeChildren(childNode, pass, null, false, canSmartExpand, forceUpdate, true, true)
                  .doWhenDone(() -> result.setResult(null));
              }
            }
          });
      });
    return result;
  }

  private void adjustSelectionOnChildRemove(@NotNull DefaultMutableTreeNode parentNode, int selectedIndex, Object disposedElement) {
    if (selectedIndex >= 0 && !getSelectedElements().isEmpty()) return;

    DefaultMutableTreeNode node = disposedElement == null ? null : getNodeForElement(disposedElement, false);
    if (node != null && isValidForSelectionAdjusting(node)) {
      Object newElement = getElementFor(node);
      addSelectionPath(getPathFor(node), true, getExpiredElementCondition(newElement), disposedElement);
      return;
    }


    if (selectedIndex >= 0) {
      if (parentNode.getChildCount() > 0) {
        if (parentNode.getChildCount() > selectedIndex) {
          TreeNode newChildNode = parentNode.getChildAt(selectedIndex);
          if (isValidForSelectionAdjusting(newChildNode)) {
            addSelectionPath(new TreePath(myTreeModel.getPathToRoot(newChildNode)), true, getExpiredElementCondition(disposedElement),
                             disposedElement);
          }
        }
        else {
          TreeNode newChild = parentNode.getChildAt(parentNode.getChildCount() - 1);
          if (isValidForSelectionAdjusting(newChild)) {
            addSelectionPath(new TreePath(myTreeModel.getPathToRoot(newChild)), true, getExpiredElementCondition(disposedElement),
                             disposedElement);
          }
        }
      }
      else {
        addSelectionPath(new TreePath(myTreeModel.getPathToRoot(parentNode)), true, getExpiredElementCondition(disposedElement),
                         disposedElement);
      }
    }
  }

  private boolean isValidForSelectionAdjusting(@NotNull TreeNode node) {
    if (!myTree.isRootVisible() && getRootNode() == node) return false;

    if (isLoadingNode(node)) return true;

    final Object elementInTree = getElementFor(node);
    if (elementInTree == null) return false;

    final TreeNode parentNode = node.getParent();
    final Object parentElementInTree = getElementFor(parentNode);
    if (parentElementInTree == null) return false;

    final Object parentElement = getTreeStructure().getParentElement(elementInTree);

    return parentElementInTree.equals(parentElement);
  }

  @NotNull
  private Condition getExpiredElementCondition(final Object element) {
    return o -> isInStructure(element);
  }

  private void addSelectionPath(@NotNull final TreePath path,
                                final boolean isAdjustedSelection,
                                final Condition isExpiredAdjustment,
                                @Nullable final Object adjustmentCause) {
    processInnerChange(new TreeRunnable2("AbstractTreeUi.addSelectionPath") {
      @Override
      public void perform() {
        TreePath toSelect = null;

        if (isLoadingNode(path.getLastPathComponent())) {
          final TreePath parentPath = path.getParentPath();
          if (parentPath != null && isValidForSelectionAdjusting((TreeNode)parentPath.getLastPathComponent())) {
            toSelect = parentPath;
          }
        }
        else {
          toSelect = path;
        }

        if (toSelect != null) {
          mySelectionIsAdjusted = isAdjustedSelection;

          myTree.addSelectionPath(toSelect);

          if (isAdjustedSelection && myUpdaterState != null) {
            final Object toSelectElement = getElementFor(toSelect.getLastPathComponent());
            myUpdaterState.addAdjustedSelection(toSelectElement, isExpiredAdjustment, adjustmentCause);
          }
        }
      }
    });
  }

  @NotNull
  private static TreePath getPathFor(@NotNull TreeNode node) {
    if (node instanceof DefaultMutableTreeNode) {
      return new TreePath(((DefaultMutableTreeNode)node).getPath());
    }
    else {
      List<TreeNode> nodes = new ArrayList<>();
      TreeNode eachParent = node;
      while (eachParent != null) {
        nodes.add(eachParent);
        eachParent = eachParent.getParent();
      }

      return new TreePath(ArrayUtil.toObjectArray(nodes));
    }
  }


  private void removeNodeFromParent(@NotNull final MutableTreeNode node, final boolean willAdjustSelection) {
    processInnerChange(new TreeRunnable2("AbstractTreeUi.removeNodeFromParent") {
      @Override
      public void perform() {
        if (willAdjustSelection) {
          final TreePath path = getPathFor(node);
          if (myTree.isPathSelected(path)) {
            myTree.removeSelectionPath(path);
          }
        }

        if (node.getParent() != null) {
          myTreeModel.removeNodeFromParent(node);
        }
      }
    });
  }

  private void expandPath(@NotNull final TreePath path, final boolean canSmartExpand) {
    processInnerChange(new TreeRunnable2("AbstractTreeUi.expandPath") {
      @Override
      public void perform() {
        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
          if (node.getChildCount() > 0 && !myTree.isExpanded(path)) {
            if (!canSmartExpand) {
              myNotForSmartExpand.add(node);
            }
            try {
              myRequestedExpand = path;
              myTree.expandPath(path);
              processSmartExpand(node, canSmartExpand, false);
            }
            finally {
              myNotForSmartExpand.remove(node);
              myRequestedExpand = null;
            }
          }
          else {
            processNodeActionsIfReady(node);
          }
        }
      }
    });
  }

  private void processInnerChange(Runnable runnable) {
    if (myUpdaterState == null) {
      setUpdaterState(new UpdaterTreeState2(this));
    }

    myUpdaterState.process(runnable);
  }

  private boolean isInnerChange() {
    return myUpdaterState != null && myUpdaterState.isProcessingNow() && myUserRunnables.isEmpty();
  }

  private void makeLoadingOrLeafIfNoChildren(@NotNull final DefaultMutableTreeNode node) {
    TreePath path = getPathFor(node);

    insertLoadingNode(node, true);

    final AbstractTreeNod2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;

    descriptor.setChildrenSortingStamp(-1);

    if (getBuilder().isAlwaysShowPlus(descriptor)) return;


    TreePath parentPath = path.getParentPath();
    if (myTree.isVisible(path) || parentPath != null && myTree.isExpanded(parentPath)) {
      if (myTree.isExpanded(path)) {
        addSubtreeToUpdate(node);
      }
      else {
        insertLoadingNode(node, false);
      }
    }
  }

  /**
   * Indicates whether the given {@code descriptor} is valid
   * and its parent is equal to the specified {@code parent}.
   *
   * @param descriptor a descriptor to test
   * @param parent     an expected parent for the testing descriptor
   * @return {@code true} if the specified descriptor is valid
   */
  private boolean isValid(NodeDescriptor2 descriptor, NodeDescriptor2 parent) {
    if (descriptor == null) return false;
    if (parent != null && parent != descriptor.getParentDescriptor()) return false;
    return isValid2(getElementFromDescriptor(descriptor));
  }

  private boolean isValid2(@Nullable NodeDescriptor2 descriptor) {
    return descriptor != null && isValid(getElementFromDescriptor(descriptor));
  }

  private boolean isValid(Object element) {
    if (isNodeNull(element)) return false;
    if (element instanceof ValidateableNode) {
      if (!((ValidateableNode)element).isValid()) return false;
    }
    return getBuilder().validateNode(element);
  }

  private void insertLoadingNode(final DefaultMutableTreeNode node, boolean addToUnbuilt) {
    if (!isLoadingChildrenFor(node)) {
      myTreeModel.insertNodeInto(new LoadingNode(), node, 0);
    }

    if (addToUnbuilt) {
      addToUnbuilt(node);
    }
  }


  @NotNull
  private Promise<Void> queueToBackground(@NotNull final Runnable bgBuildAction, @Nullable final Runnable edtPostRunnable) {
    if (!canInitiateNewActivity()) return Promises.rejectedPromise();
    final AsyncPromise<Void> result = new AsyncPromise<>();
    final AtomicReference<ProcessCanceledException> fail = new AtomicReference<>();
    final Runnable finalizer = new TreeRunnable2("AbstractTreeUi.queueToBackground: finalizer") {
      @Override
      public void perform() {
        ProcessCanceledException exception = fail.get();
        if (exception == null) {
          result.setResult(null);
        }
        else {
          result.setError(exception);
        }
      }
    };

    registerWorkerTask(bgBuildAction);

    final Runnable pooledThreadWithProgressRunnable = new TreeRunnable2("AbstractTreeUi.queueToBackground: progress") {
      @Override
      public void perform() {
        try {
          final AbstractTreeBuilder2 builder = getBuilder();

          if (!canInitiateNewActivity()) {
            throw new ProcessCanceledException();
          }

          builder.runBackgroundLoading(new TreeRunnable2("AbstractTreeUi.queueToBackground: background") {
            @Override
            public void perform() {
              assertNotDispatchThread();
              try {
                if (!canInitiateNewActivity()) {
                  throw new ProcessCanceledException();
                }

                execute(bgBuildAction);

                if (edtPostRunnable != null) {
                  builder.updateAfterLoadedInBackground(new TreeRunnable2("AbstractTreeUi.queueToBackground: update after") {
                    @Override
                    public void perform() {
                      try {
                        assertIsDispatchThread();
                        if (!canInitiateNewActivity()) {
                          throw new ProcessCanceledException();
                        }

                        execute(edtPostRunnable);

                      }
                      catch (ProcessCanceledException e) {
                        fail.set(e);
                        cancelUpdate();
                      }
                      finally {
                        unregisterWorkerTask(bgBuildAction, finalizer);
                      }
                    }
                  });
                }
                else {
                  unregisterWorkerTask(bgBuildAction, finalizer);
                }
              }
              catch (ProcessCanceledException e) {
                fail.set(e);
                unregisterWorkerTask(bgBuildAction, finalizer);
                cancelUpdate();
              }
              catch (Throwable t) {
                unregisterWorkerTask(bgBuildAction, finalizer);
                throw new RuntimeException(t);
              }
            }
          });
        }
        catch (ProcessCanceledException e) {
          unregisterWorkerTask(bgBuildAction, finalizer);
          cancelUpdate();
        }
      }
    };

    Runnable pooledThreadRunnable = new TreeRunnable2("AbstractTreeUi.queueToBackground") {
      @Override
      public void perform() {
        try {
          if (myProgress != null && ProgressManager.getGlobalProgressIndicator() != myProgress) {
            ProgressManager.getInstance().runProcess(pooledThreadWithProgressRunnable, myProgress);
          }
          else {
            execute(pooledThreadWithProgressRunnable);
          }
        }
        catch (ProcessCanceledException e) {
          fail.set(e);
          unregisterWorkerTask(bgBuildAction, finalizer);
          cancelUpdate();
        }
      }
    };

    if (isPassthroughMode()) {
      execute(pooledThreadRunnable);
    }
    else {
      myWorker.addFirst(pooledThreadRunnable);
    }
    return result;
  }

  private void registerWorkerTask(@NotNull Runnable runnable) {
    synchronized (myActiveWorkerTasks) {
      myActiveWorkerTasks.add(runnable);
    }
  }

  private void unregisterWorkerTask(@NotNull Runnable runnable, @Nullable Runnable finalizeRunnable) {
    boolean wasRemoved;

    synchronized (myActiveWorkerTasks) {
      wasRemoved = myActiveWorkerTasks.remove(runnable);
    }

    if (wasRemoved && finalizeRunnable != null) {
      finalizeRunnable.run();
    }

    invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.unregisterWorkerTask") {
      @Override
      public void perform() {
        maybeReady();
      }
    });
  }

  private boolean isWorkerBusy() {
    synchronized (myActiveWorkerTasks) {
      return !myActiveWorkerTasks.isEmpty();
    }
  }

  private void clearWorkerTasks() {
    synchronized (myActiveWorkerTasks) {
      myActiveWorkerTasks.clear();
    }
  }

  private void updateNodeImageAndPosition(@NotNull final DefaultMutableTreeNode node) {
    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;
    if (getElementFromDescriptor(descriptor) == null) return;

    nodeChanged(node);
  }

  private void nodeChanged(final DefaultMutableTreeNode node) {
    invokeLaterIfNeeded(true, new TreeRunnable2("AbstractTreeUi.nodeChanged") {
      @Override
      public void perform() {
        myTreeModel.nodeChanged(node);
      }
    });
  }

  private void nodeStructureChanged(final DefaultMutableTreeNode node) {
    invokeLaterIfNeeded(true, new TreeRunnable2("AbstractTreeUi.nodeStructureChanged") {
      @Override
      public void perform() {
        myTreeModel.nodeStructureChanged(node);
      }
    });
  }

  private void insertNodesInto(@NotNull final List<? extends TreeNode> toInsert, @NotNull final DefaultMutableTreeNode parentNode) {
    sortChildren(parentNode, toInsert, false, true);
    final List<TreeNode> all = new ArrayList<>(toInsert.size() + parentNode.getChildCount());
    all.addAll(toInsert);
    all.addAll(TreeUtil.listChildren(parentNode));

    if (!toInsert.isEmpty()) {
      sortChildren(parentNode, all, true, true);

      int[] newNodeIndices = new int[toInsert.size()];
      int eachNewNodeIndex = 0;
      TreeMap<Integer, TreeNode> insertSet = new TreeMap<>();
      for (int i = 0; i < toInsert.size(); i++) {
        TreeNode eachNewNode = toInsert.get(i);
        while (all.get(eachNewNodeIndex) != eachNewNode) {
          eachNewNodeIndex++;
        }
        newNodeIndices[i] = eachNewNodeIndex;
        insertSet.put(eachNewNodeIndex, eachNewNode);
      }

      for (Map.Entry<Integer, TreeNode> entry : insertSet.entrySet()) {
        TreeNode eachNode = entry.getValue();
        Integer index = entry.getKey();
        parentNode.insert((MutableTreeNode)eachNode, index);
      }

      myTreeModel.nodesWereInserted(parentNode, newNodeIndices);
    }
    else {
      List<TreeNode> before = new ArrayList<>(all);

      sortChildren(parentNode, all, true, false);
      if (!before.equals(all)) {
        processInnerChange(new TreeRunnable2("AbstractTreeUi.insertNodesInto") {
          @Override
          public void perform() {
            Enumeration<TreePath> expanded = getTree().getExpandedDescendants(getPathFor(parentNode));
            TreePath[] selected = getTree().getSelectionModel().getSelectionPaths();

            parentNode.removeAllChildren();
            for (TreeNode each : all) {
              parentNode.add((MutableTreeNode)each);
            }
            nodeStructureChanged(parentNode);

            if (expanded != null) {
              while (expanded.hasMoreElements()) {
                expandSilently(expanded.nextElement());
              }
            }

            if (selected != null) {
              for (TreePath each : selected) {
                if (!getTree().getSelectionModel().isPathSelected(each)) {
                  addSelectionSilently(each);
                }
              }
            }
          }
        });
      }
    }
  }

  private void sortChildren(@NotNull DefaultMutableTreeNode node, @NotNull List<? extends TreeNode> children, boolean updateStamp, boolean forceSort) {
    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    assert descriptor != null;

    if (descriptor.getChildrenSortingStamp() >= getComparatorStamp() && !forceSort) return;
    if (!children.isEmpty()) {
      try {
        children.sort(myNodeComparator);
      }
      catch (IllegalArgumentException exception) {
        StringBuilder sb = new StringBuilder("cannot sort children in ").append(toString());
        children.forEach(child -> sb.append('\n').append(child));
        throw new IllegalArgumentException(sb.toString(), exception);
      }
    }

    if (updateStamp) {
      descriptor.setChildrenSortingStamp(getComparatorStamp());
    }
  }

  private void disposeNode(@NotNull DefaultMutableTreeNode node) {
    TreeNode parent = node.getParent();
    if (parent instanceof DefaultMutableTreeNode) {
      addToUnbuilt((DefaultMutableTreeNode)parent);
    }

    if (node.getChildCount() > 0) {
      for (DefaultMutableTreeNode _node = (DefaultMutableTreeNode)node.getFirstChild(); _node != null; _node = _node.getNextSibling()) {
        disposeNode(_node);
      }
    }

    removeFromUpdatingChildren(node);
    removeFromUnbuilt(node);
    removeFromCancelled(node);

    if (isLoadingNode(node)) return;
    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    if (descriptor == null) return;
    final Object element = getElementFromDescriptor(descriptor);
    if (!isNodeNull(element)) {
      removeMapping(element, node, null);
    }
    node.setUserObject(null);
    node.removeAllChildren();
  }

  public void addSubtreeToUpdate(@NotNull final DefaultMutableTreeNode root) {
    addSubtreeToUpdate(root, true);
  }

  public void addSubtreeToUpdate(@NotNull final DefaultMutableTreeNode root, boolean updateStructure) {
    addSubtreeToUpdate(root, null, updateStructure);
  }

  public void addSubtreeToUpdate(@NotNull final DefaultMutableTreeNode root, @Nullable final Runnable runAfterUpdate, final boolean updateStructure) {
    final Object element = getElementFor(root);
    final boolean alwaysLeaf = element != null && getTreeStructure().isAlwaysLeaf(element);
    final TreeUpdatePass2 updatePass;
    if (alwaysLeaf) {
      removeFromUnbuilt(root);
      removeLoading(root, true);
      updatePass = new TreeUpdatePass2(root).setUpdateChildren(false);
    }
    else {
      updatePass = new TreeUpdatePass2(root).setUpdateStructure(updateStructure).setUpdateStamp(-1);
    }
    final AbstractTreeUpdater2 updater = getUpdater();
    updater.runAfterUpdate(runAfterUpdate);
    updater.addSubtreeToUpdate(updatePass);
  }

  boolean wasRootNodeInitialized() {
    return myRootNodeWasQueuedToInitialize && myRootNodeInitialized;
  }

  public void select(final Object /*@NotNull*/ [] elements, @Nullable final Runnable onDone, boolean addToSelection, boolean deferred) {
    _select(elements, onDone, addToSelection, true, false, true, deferred, false, false);
  }

  void _select(final Object /*@NotNull*/ [] elements,
               final Runnable onDone,
               final boolean addToSelection,
               final boolean checkIfInStructure) {

    _select(elements, onDone, addToSelection, true, checkIfInStructure, true, false, false, false);
  }

  void _select(final Object /*@NotNull*/ [] elements,
               @NotNull Runnable onDone) {

    _select(elements, onDone, false, true, true, false, false, false, false);
  }

  public void userSelect(final Object /*@NotNull*/ [] elements, final Runnable onDone, final boolean addToSelection, boolean scroll) {
    _select(elements, onDone, addToSelection, true, false, scroll, false, true, true);
  }

  void _select(final Object /*@NotNull*/ [] elements,
               final Runnable onDone,
               final boolean addToSelection,
               final boolean checkCurrentSelection,
               final boolean checkIfInStructure,
               final boolean scrollToVisible,
               final boolean deferred,
               final boolean canSmartExpand,
               final boolean mayQueue) {

    assertIsDispatchThread();

    AbstractTreeUpdater2 updater = getUpdater();
    if (mayQueue && updater != null) {
      updater.queueSelection(
        new SelectionRequest2(elements, onDone, addToSelection, checkCurrentSelection, checkIfInStructure, scrollToVisible, deferred,
                             canSmartExpand));
      return;
    }


    boolean willAffectSelection = elements.length > 0 || addToSelection;
    if (!willAffectSelection) {
      runDone(onDone);
      maybeReady();
      return;
    }

    final boolean oldCanProcessDeferredSelection = myCanProcessDeferredSelections;

    if (!deferred && wasRootNodeInitialized()) {
      _getReady().doWhenDone(new TreeRunnable2("AbstractTreeUi._select: on done getReady") {
        @Override
        public void perform() {
          myCanProcessDeferredSelections = false;
        }
      });
    }

    if (!checkDeferred(deferred, onDone)) return;

    if (!deferred && oldCanProcessDeferredSelection && !myCanProcessDeferredSelections) {
      if (!addToSelection) {
        getTree().clearSelection();
      }
    }


    runDone(new TreeRunnable2("AbstractTreeUi._select") {
      @Override
      public void perform() {
        try {
          if (!checkDeferred(deferred, onDone)) return;

          final Set<Object> currentElements = getSelectedElements();

          if (checkCurrentSelection && !currentElements.isEmpty() && elements.length == currentElements.size()) {
            boolean runSelection = false;
            for (Object eachToSelect : elements) {
              if (!currentElements.contains(eachToSelect)) {
                runSelection = true;
                break;
              }
            }

            if (!runSelection) {
              selectVisible(elements[0], onDone, false, false, scrollToVisible);
              return;
            }
          }

          clearSelection();
          Set<Object> toSelect = new THashSet<>();
          ContainerUtil.addAllNotNull(toSelect, elements);
          if (addToSelection) {
            ContainerUtil.addAllNotNull(toSelect, currentElements);
          }

          if (checkIfInStructure) {
            toSelect.removeIf(each -> !isInStructure(each));
          }

          final Object[] elementsToSelect = ArrayUtil.toObjectArray(toSelect);

          if (wasRootNodeInitialized()) {
            final int[] originalRows = myTree.getSelectionRows();
            if (!addToSelection) {
              clearSelection();
            }
            addNext(elementsToSelect, 0, new TreeRunnable2("AbstractTreeUi._select: addNext") {
              @Override
              public void perform() {
                if (getTree().isSelectionEmpty()) {
                  processInnerChange(new TreeRunnable2("AbstractTreeUi._select: addNext: processInnerChange") {
                    @Override
                    public void perform() {
                      restoreSelection(currentElements);
                    }
                  });
                }
                runDone(onDone);
              }
            }, originalRows, deferred, scrollToVisible, canSmartExpand);
          }
          else {
            addToDeferred(elementsToSelect, onDone, addToSelection);
          }
        }
        finally {
          maybeReady();
        }
      }
    });
  }

  private void clearSelection() {
    myTree.clearSelection();
  }

  private void restoreSelection(@NotNull Set<Object> selection) {
    for (Object each : selection) {
      DefaultMutableTreeNode node = getNodeForElement(each, false);
      if (node != null && isValidForSelectionAdjusting(node)) {
        addSelectionPath(getPathFor(node), false, null, null);
      }
    }
  }


  private void addToDeferred(final Object /*@NotNull*/ [] elementsToSelect, final Runnable onDone, final boolean addToSelection) {
    if (!addToSelection) {
      myDeferredSelections.clear();
    }
    myDeferredSelections.add(new TreeRunnable2("AbstractTreeUi.addToDeferred") {
      @Override
      public void perform() {
        select(elementsToSelect, onDone, addToSelection, true);
      }
    });
  }

  private boolean checkDeferred(boolean isDeferred, @Nullable Runnable onDone) {
    if (!isDeferred || myCanProcessDeferredSelections || !wasRootNodeInitialized()) {
      return true;
    }
    else {
      runDone(onDone);
      return false;
    }
  }

  @NotNull
  final Set<Object> getSelectedElements() {
    TreePath[] paths = myTree.getSelectionPaths();

    Set<Object> result = new LinkedHashSet<>();
    if (paths != null) {
      for (TreePath eachPath : paths) {
        if (eachPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode eachNode = (DefaultMutableTreeNode)eachPath.getLastPathComponent();
          if (eachNode == myRootNode && !myTree.isRootVisible()) continue;
          Object eachElement = getElementFor(eachNode);
          if (eachElement != null) {
            result.add(eachElement);
          }
        }
      }
    }
    return result;
  }


  private void addNext(final Object /*@NotNull*/ [] elements,
                       final int i,
                       @Nullable final Runnable onDone,
                       final int[] originalRows,
                       final boolean deferred,
                       final boolean scrollToVisible,
                       final boolean canSmartExpand) {
    if (i >= elements.length) {
      if (myTree.isSelectionEmpty()) {
        myTree.setSelectionRows(originalRows);
      }
      runDone(onDone);
    }
    else {
      if (!checkDeferred(deferred, onDone)) {
        return;
      }

      doSelect(elements[i], new TreeRunnable2("AbstractTreeUi.addNext") {
        @Override
        public void perform() {
          if (!checkDeferred(deferred, onDone)) return;

          addNext(elements, i + 1, onDone, originalRows, deferred, scrollToVisible, canSmartExpand);
        }
      }, deferred, i == 0, scrollToVisible, canSmartExpand);
    }
  }

  public void select(@Nullable Object element, @Nullable final Runnable onDone, boolean addToSelection) {
     if (element == null) return;
    _select(new Object[]{element}, onDone, addToSelection, false);
  }

  private void doSelect(@NotNull final Object element,
                        final Runnable onDone,
                        final boolean deferred,
                        final boolean canBeCentered,
                        final boolean scrollToVisible,
                        final boolean canSmartExpand) {
    final Runnable _onDone = new TreeRunnable2("AbstractTreeUi.doSelect") {
      @Override
      public void perform() {
        if (!checkDeferred(deferred, onDone)) return;

        checkPathAndMaybeRevalidate(element, new TreeRunnable2("AbstractTreeUi.doSelect: checkPathAndMaybeRevalidate") {
          @Override
          public void perform() {
            selectVisible(element, onDone, true, canBeCentered, scrollToVisible);
          }
        }, true, canSmartExpand);
      }
    };
    _expand(element, _onDone, true, false, canSmartExpand);
  }

  private void checkPathAndMaybeRevalidate(@NotNull Object element,
                                           @NotNull final Runnable onDone,
                                           final boolean parentsOnly,
                                           final boolean canSmartExpand) {
    boolean toRevalidate = isValid(element) && !myRevalidatedObjects.contains(element) && getNodeForElement(element, false) == null && isInStructure(element);
    if (!toRevalidate) {
      runDone(onDone);
      return;
    }

    myRevalidatedObjects.add(element);
    getBuilder()
      .revalidateElement(element)
      .onSuccess(o -> invokeLaterIfNeeded(false, new TreeRunnable2("AbstractTreeUi.checkPathAndMaybeRevalidate: on done revalidateElement") {
        @Override
        public void perform() {
          _expand(o, onDone, parentsOnly, false, canSmartExpand);
        }
      }))
      .onError(throwable -> wrapDone(onDone, "AbstractTreeUi.checkPathAndMaybeRevalidate: on rejected revalidateElement").run());
  }

  private void selectVisible(@NotNull Object element, final Runnable onDone, boolean addToSelection, boolean canBeCentered, final boolean scroll) {
    DefaultMutableTreeNode toSelect = getNodeToScroll(element);
    if (toSelect == null) {
      runDone(onDone);
      return;
    }
    if (myUpdaterState != null) {
      myUpdaterState.addSelection(element);
    }
    setHoldSize(false);
    runDone(wrapScrollTo(onDone, element, toSelect, addToSelection, canBeCentered, scroll));
  }

  void userScrollTo(Object element, Runnable onDone) {
    DefaultMutableTreeNode node = getNodeToScroll(element);
    runDone(node == null ? onDone : wrapScrollTo(onDone, element, node, false, true, true));
  }

  private DefaultMutableTreeNode getNodeToScroll(Object element) {
    if (element == null) return null;
    DefaultMutableTreeNode node = getNodeForElement(element, false);
    if (node == null) return null;
    return myTree.isRootVisible() || node != getRootNode() ? node : null;
  }

  @NotNull
  private Runnable wrapDone(Runnable onDone, @NotNull String name) {
    return new TreeRunnable2(name) {
      @Override
      public void perform() {
        runDone(onDone);
      }
    };
  }

  @NotNull
  private Runnable wrapScrollTo(Runnable onDone,
                                @NotNull Object element,
                                @NotNull DefaultMutableTreeNode node,
                                boolean addToSelection,
                                boolean canBeCentered,
                                boolean scroll) {
    return new TreeRunnable2("AbstractTreeUi.wrapScrollTo") {
      @Override
      public void perform() {
        int row = getRowIfUnderSelection(element);
        if (row == -1) row = myTree.getRowForPath(new TreePath(node.getPath()));
        int top = row - 2;
        int bottom = row + 2;
        if (canBeCentered && Registry.is("ide.tree.autoscrollToVCenter")) {
          int count = TreeUtil.getVisibleRowCount(myTree) - 1;
          top = count > 0 ? row - count / 2 : row;
          bottom = count > 0 ? top + count : row;
        }
        TreeUtil.showAndSelect(myTree, top, bottom, row, -1, addToSelection, scroll)
          .doWhenDone(wrapDone(onDone, "AbstractTreeUi.wrapScrollTo.onDone"));
      }
    };
  }

  private int getRowIfUnderSelection(@NotNull Object element) {
    final Set<Object> selection = getSelectedElements();

    if (selection.contains(element)) {
      final TreePath[] paths = getTree().getSelectionPaths();
      for (TreePath each : paths) {
        if (element.equals(getElementFor(each.getLastPathComponent()))) {
          return getTree().getRowForPath(each);
        }
      }
      return -1;
    }

    Object anchor = TreeAnchorizer.getService().createAnchor(element);
    Object o = isNodeNull(anchor) ? null : myElementToNodeMap.get(anchor);
    TreeAnchorizer.getService().freeAnchor(anchor);

    if (o instanceof List) {
      final TreePath[] paths = getTree().getSelectionPaths();
      if (paths != null && paths.length > 0) {
        Set<DefaultMutableTreeNode> selectedNodes = new HashSet<>();
        for (TreePath eachPAth : paths) {
          if (eachPAth.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            selectedNodes.add((DefaultMutableTreeNode)eachPAth.getLastPathComponent());
          }
        }


        //noinspection unchecked
        for (DefaultMutableTreeNode eachNode : (List<DefaultMutableTreeNode>)o) {
          while (eachNode != null) {
            if (selectedNodes.contains(eachNode)) {
              return getTree().getRowForPath(getPathFor(eachNode));
            }
            eachNode = (DefaultMutableTreeNode)eachNode.getParent();
          }
        }
      }
    }

    return -1;
  }

  public void expand(final Object element, @Nullable final Runnable onDone) {
    expand(new Object[]{element}, onDone);
  }

  public void expand(final Object /*@NotNull*/ [] element, @Nullable final Runnable onDone) {
    expand(element, onDone, false);
  }


  void expand(final Object /*@NotNull*/ [] element, @Nullable final Runnable onDone, boolean checkIfInStructure) {
    _expand(element, onDone == null ? new EmptyRunnable() : onDone, checkIfInStructure);
  }

  private void _expand(final Object /*@NotNull*/ [] elements,
                       @NotNull final Runnable onDone,
                       final boolean checkIfInStructure) {

    try {
      runDone(new TreeRunnable2("AbstractTreeUi._expand") {
        @Override
        public void perform() {
          if (elements.length == 0) {
            runDone(onDone);
            return;
          }

          if (myUpdaterState != null) {
            myUpdaterState.clearExpansion();
          }


          final ActionCallback done = new ActionCallback(elements.length);
          done
            .doWhenDone(wrapDone(onDone, "AbstractTreeUi._expand: on done expandNext"))
            .doWhenRejected(wrapDone(onDone, "AbstractTreeUi._expand: on rejected expandNext"));

          expandNext(elements, 0, false, checkIfInStructure, false, done, 0);
        }
      });
    }
    catch (ProcessCanceledException e) {
      try {
        runDone(onDone);
      }
      catch (ProcessCanceledException ignored) {
        //todo[kirillk] added by Nik to fix IDEA-58475. I'm not sure that it is correct solution
      }
    }
  }

  private void expandNext(final Object /*@NotNull*/ [] elements,
                          final int index,
                          final boolean parentsOnly,
                          final boolean checkIfInStricture,
                          final boolean canSmartExpand,
                          @NotNull final ActionCallback done,
                          final int currentDepth) {
    if (elements.length <= 0) {
      done.setDone();
      return;
    }

    if (index >= elements.length) {
      return;
    }

    final int[] actualDepth = {currentDepth};
    boolean breakCallChain = false;
    if (actualDepth[0] > Registry.intValue("ide.tree.expandRecursionDepth")) {
      actualDepth[0] = 0;
      breakCallChain = true;
    }

    Runnable expandRunnable = new TreeRunnable2("AbstractTreeUi.expandNext") {
      @Override
      public void perform() {
        _expand(elements[index], new TreeRunnable2("AbstractTreeUi.expandNext: on done") {
          @Override
          public void perform() {
            done.setDone();
            expandNext(elements, index + 1, parentsOnly, checkIfInStricture, canSmartExpand, done, actualDepth[0] + 1);
          }
        }, parentsOnly, checkIfInStricture, canSmartExpand);
      }
    };

    if (breakCallChain && !isPassthroughMode()) {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(expandRunnable);
    }
    else {
      expandRunnable.run();
    }
  }

  public void collapseChildren(@NotNull final Object element, @Nullable final Runnable onDone) {
    runDone(new TreeRunnable2("AbstractTreeUi.collapseChildren") {
      @Override
      public void perform() {
        final DefaultMutableTreeNode node = getNodeForElement(element, false);
        if (node != null) {
          getTree().collapsePath(new TreePath(node.getPath()));
          runDone(onDone);
        }
      }
    });
  }

  private void runDone(@Nullable Runnable done) {
    if (done == null) return;

    if (!canInitiateNewActivity()) {
      if (done instanceof AbstractTreeBuilder.UserRunnable) {
        return;
      }
    }

    if (isYeildingNow()) {
      myYieldingDoneRunnables.add(done);
    }
    else {
      try {
        execute(done);
      }
      catch (ProcessCanceledException ignored) {
      }
    }
  }

  private void _expand(final Object element,
                       @NotNull final Runnable onDone,
                       final boolean parentsOnly,
                       boolean checkIfInStructure,
                       boolean canSmartExpand) {

    if (checkIfInStructure && !isInStructure(element)) {
      runDone(onDone);
      return;
    }

    if (wasRootNodeInitialized()) {
      List<Object> kidsToExpand = new ArrayList<>();
      Object eachElement = element;
      DefaultMutableTreeNode firstVisible = null;
      while (true) {
        if (eachElement == null || !isValid(eachElement)) break;

        final int preselected = getRowIfUnderSelection(eachElement);
        if (preselected >= 0) {
          firstVisible = (DefaultMutableTreeNode)getTree().getPathForRow(preselected).getLastPathComponent();
        }
        else {
          firstVisible = getNodeForElement(eachElement, true);
        }


        if (eachElement != element || !parentsOnly) {
          kidsToExpand.add(eachElement);
        }
        if (firstVisible != null) break;
        eachElement = getTreeStructure().getParentElement(eachElement);
        if (eachElement == null) break;

        int i = kidsToExpand.indexOf(eachElement);
        if (i != -1) {
          try {
            Object existing = kidsToExpand.get(i);
            LOG.error("Tree path contains equal elements at different levels:\n" +
                      " element: '" + eachElement + "'; " + eachElement.getClass() + " ("+System.identityHashCode(eachElement)+");\n" +
                      "existing: '" + existing + "'; " + existing.getClass()+ " ("+System.identityHashCode(existing)+"); " +
                      "path='" + kidsToExpand + "'; tree structure=" + myTreeStructure);
          }
          catch (AssertionError ignored) {
          }
          runDone(onDone);
          throw new ProcessCanceledException();
        }
      }


      if (firstVisible == null) {
        runDone(onDone);
      }
      else if (kidsToExpand.isEmpty()) {
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)firstVisible.getParent();
        if (parentNode != null) {
          final TreePath parentPath = new TreePath(parentNode.getPath());
          if (!myTree.isExpanded(parentPath)) {
            expand(parentPath, canSmartExpand);
          }
        }
        runDone(onDone);
      }
      else {
        processExpand(firstVisible, kidsToExpand, kidsToExpand.size() - 1, onDone, canSmartExpand);
      }
    }
    else {
      deferExpansion(element, onDone, parentsOnly, canSmartExpand);
    }
  }

  private void deferExpansion(final Object element, @NotNull final Runnable onDone, final boolean parentsOnly, final boolean canSmartExpand) {
    myDeferredExpansions.add(new TreeRunnable2("AbstractTreeUi.deferExpansion") {
      @Override
      public void perform() {
        _expand(element, onDone, parentsOnly, false, canSmartExpand);
      }
    });
  }

  private void processExpand(final DefaultMutableTreeNode toExpand,
                             @NotNull final List<Object> kidsToExpand,
                             final int expandIndex,
                             @NotNull final Runnable onDone,
                             final boolean canSmartExpand) {

    final Object element = getElementFor(toExpand);
    if (element == null) {
      runDone(onDone);
      return;
    }

    addNodeAction(element, true, node -> {
      if (node.getChildCount() > 0 && !myTree.isExpanded(new TreePath(node.getPath()))) {
        expand(node, canSmartExpand);
      }

      if (expandIndex <= 0) {
        runDone(onDone);
        return;
      }

      checkPathAndMaybeRevalidate(kidsToExpand.get(expandIndex - 1), new TreeRunnable2("AbstractTreeUi.processExpand") {
        @Override
        public void perform() {
          final DefaultMutableTreeNode nextNode = getNodeForElement(kidsToExpand.get(expandIndex - 1), false);
          processExpand(nextNode, kidsToExpand, expandIndex - 1, onDone, canSmartExpand);
        }
      }, false, canSmartExpand);
    });


    boolean childrenToUpdate = areChildrenToBeUpdated(toExpand);
    boolean expanded = myTree.isExpanded(getPathFor(toExpand));
    boolean unbuilt = myUnbuiltNodes.contains(toExpand);

    if (expanded) {
      if (unbuilt || childrenToUpdate) {
        addSubtreeToUpdate(toExpand);
      }
    }
    else {
      expand(toExpand, canSmartExpand);
    }

    if (!unbuilt && !childrenToUpdate) {
      processNodeActionsIfReady(toExpand);
    }
  }

  private boolean areChildrenToBeUpdated(DefaultMutableTreeNode node) {
    return getUpdater().isEnqueuedToUpdate(node) || isUpdatingParent(node) || myCancelledBuild.containsKey(node);
  }

  @Nullable
  public Object getElementFor(Object node) {
    NodeDescriptor2 descriptor = getDescriptorFrom(node);
    return descriptor == null ? null : getElementFromDescriptor(descriptor);
  }

  private boolean isNodeBeingBuilt(@NotNull Object node) {
    return getParentBuiltNode(node) != null || myRootNode == node && !wasRootNodeInitialized();
  }

  @Nullable
  private DefaultMutableTreeNode getParentBuiltNode(@NotNull Object node) {
    DefaultMutableTreeNode parent = getParentLoadingInBackground(node);
    if (parent != null) return parent;

    if (isLoadingParentInBackground(node)) return (DefaultMutableTreeNode)node;

    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)node;

    final boolean childrenAreNoLoadedYet = myUnbuiltNodes.contains(treeNode) || isUpdatingChildrenNow(treeNode);
    if (childrenAreNoLoadedYet) {
      final TreePath nodePath = new TreePath(treeNode.getPath());
      if (!myTree.isExpanded(nodePath)) return null;

      return (DefaultMutableTreeNode)node;
    }


    return null;
  }

  private boolean isLoadingParentInBackground(Object node) {
    return node instanceof DefaultMutableTreeNode && isLoadedInBackground(getElementFor(node));
  }

  public AbstractTreeUpdater2 getUpdater() {
    return myUpdater;
  }

  public void setUpdater(@Nullable final AbstractTreeUpdater2 updater) {
    myUpdater = updater;
    if (updater != null && myUpdateIfInactive) {
      updater.showNotify();
    }

    if (myUpdater != null) {
      myUpdater.setPassThroughMode(myPassThroughMode);
    }
  }

  public DefaultMutableTreeNode getRootNode() {
    return myRootNode;
  }

  public void setRootNode(@NotNull final DefaultMutableTreeNode rootNode) {
    myRootNode = rootNode;
  }

  private void dropUpdaterStateIfExternalChange() {
    if (!isInnerChange()) {
      clearUpdaterState();
      mySelectionIsAdjusted = false;
    }
  }

  void clearUpdaterState() {
    myUpdaterState = null;
  }

  private void createMapping(@NotNull Object element, DefaultMutableTreeNode node) {
    element = TreeAnchorizer.getService().createAnchor(element);
    warnMap("myElementToNodeMap: createMapping: ", myElementToNodeMap);
    if (!myElementToNodeMap.containsKey(element)) {
      myElementToNodeMap.put(element, node);
    }
    else {
      final Object value = myElementToNodeMap.get(element);
      final List<DefaultMutableTreeNode> nodes;
      if (value instanceof DefaultMutableTreeNode) {
        nodes = new ArrayList<>();
        nodes.add((DefaultMutableTreeNode)value);
        myElementToNodeMap.put(element, nodes);
      }
      else {
        nodes = (List<DefaultMutableTreeNode>)value;
      }
      nodes.add(node);
    }
  }

  private void removeMapping(@NotNull Object element, DefaultMutableTreeNode node, @Nullable Object elementToPutNodeActionsFor) {
    element = TreeAnchorizer.getService().createAnchor(element);
    warnMap("myElementToNodeMap: removeMapping: ", myElementToNodeMap);
    final Object value = myElementToNodeMap.get(element);
    if (value != null) {
      if (value instanceof DefaultMutableTreeNode) {
        if (value.equals(node)) {
          myElementToNodeMap.remove(element);
        }
      }
      else {
        List<DefaultMutableTreeNode> nodes = (List<DefaultMutableTreeNode>)value;
        final boolean reallyRemoved = nodes.remove(node);
        if (reallyRemoved) {
          if (nodes.isEmpty()) {
            myElementToNodeMap.remove(element);
          }
        }
      }
    }

    remapNodeActions(element, elementToPutNodeActionsFor);
    TreeAnchorizer.getService().freeAnchor(element);
  }

  private void remapNodeActions(Object element, Object elementToPutNodeActionsFor) {
    _remapNodeActions(element, elementToPutNodeActionsFor, myNodeActions);
    _remapNodeActions(element, elementToPutNodeActionsFor, myNodeChildrenActions);
    warnMap("myNodeActions: remapNodeActions: ", myNodeActions);
    warnMap("myNodeChildrenActions: remapNodeActions: ", myNodeChildrenActions);
  }

  private static void _remapNodeActions(Object element, @Nullable Object elementToPutNodeActionsFor, @NotNull final Map<Object, List<NodeAction>> nodeActions) {
    final List<NodeAction> actions = nodeActions.get(element);
    nodeActions.remove(element);

    if (elementToPutNodeActionsFor != null && actions != null) {
      nodeActions.put(elementToPutNodeActionsFor, actions);
    }
  }

  @Nullable
  private DefaultMutableTreeNode getFirstNode(@NotNull Object element) {
    return findNode(element, 0);
  }

  @Nullable
  private DefaultMutableTreeNode findNode(@NotNull Object element, int startIndex) {
    final Object value = getBuilder().findNodeByElement(element);
    if (value == null) {
      return null;
    }
    if (value instanceof DefaultMutableTreeNode) {
      return startIndex == 0 ? (DefaultMutableTreeNode)value : null;
    }
    final List<DefaultMutableTreeNode> nodes = (List<DefaultMutableTreeNode>)value;
    return startIndex < nodes.size() ? nodes.get(startIndex) : null;
  }

  Object findNodeByElement(Object element) {
    element = TreeAnchorizer.getService().createAnchor(element);
    try {
      if (isNodeNull(element)) {
        return null;
      }
      if (myElementToNodeMap.containsKey(element)) {
        return myElementToNodeMap.get(element);
      }
      return null;
    }
    finally {
      TreeAnchorizer.getService().freeAnchor(element);
    }
  }

  private void addNodeAction(Object element, boolean shouldChildrenBeReady, @NotNull NodeAction action) {
    _addNodeAction(element, action, myNodeActions);
    if (shouldChildrenBeReady) {
      _addNodeAction(element, action, myNodeChildrenActions);
    }
    warnMap("myNodeActions: addNodeAction: ", myNodeActions);
    warnMap("myNodeChildrenActions: addNodeAction: ", myNodeChildrenActions);
  }


  public void addActivity() {
    if (myActivityMonitor != null) {
      myActivityMonitor.addActivity(myActivityId, getUpdater().getModalityState());
    }
  }

  private void removeActivity() {
    if (myActivityMonitor != null) {
      myActivityMonitor.removeActivity(myActivityId);
    }
  }

  private void _addNodeAction(Object element, NodeAction action, @NotNull Map<Object, List<NodeAction>> map) {
    maybeSetBusyAndScheduleWaiterForReady(true, element);
    map.computeIfAbsent(element, k -> new ArrayList<>()).add(action);

    addActivity();
  }


  private void cleanUpNow() {
    if (!canInitiateNewActivity()) return;

    final UpdaterTreeState2 state = new UpdaterTreeState2(this);

    myTree.collapsePath(new TreePath(myTree.getModel().getRoot()));
    clearSelection();
    getRootNode().removeAllChildren();
    myRootNodeWasQueuedToInitialize = false;
    myRootNodeInitialized = false;

    clearNodeActions();
    myElementToNodeMap.clear();
    myDeferredSelections.clear();
    myDeferredExpansions.clear();
    myLoadedInBackground.clear();
    myUnbuiltNodes.clear();
    myUpdateFromRootRequested = true;

    myWorker.clear();
    myTree.invalidate();

    state.restore(null);
  }

  public void setClearOnHideDelay(final long clearOnHideDelay) {
    myClearOnHideDelay = clearOnHideDelay;
  }

  private class MySelectionListener implements TreeSelectionListener {
    @Override
    public void valueChanged(@NotNull final TreeSelectionEvent e) {
      if (mySilentSelect != null && mySilentSelect.equals(e.getNewLeadSelectionPath())) return;

      dropUpdaterStateIfExternalChange();
    }
  }


  private class MyExpansionListener implements TreeExpansionListener {
    @Override
    public void treeExpanded(@NotNull TreeExpansionEvent event) {
      final TreePath path = event.getPath();

      if (mySilentExpand != null && mySilentExpand.equals(path)) return;

      dropUpdaterStateIfExternalChange();

      if (myRequestedExpand != null && !myRequestedExpand.equals(path)) {
        _getReady().doWhenDone(new TreeRunnable2("AbstractTreeUi.MyExpansionListener.treeExpanded") {
          @Override
          public void perform() {
            Object element = getElementFor(path.getLastPathComponent());
            expand(element, null);
          }
        });
        return;
      }


      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();

      if (!myUnbuiltNodes.contains(node)) {
        removeLoading(node, false);

        Set<DefaultMutableTreeNode> childrenToUpdate = new HashSet<>();
        for (int i = 0; i < node.getChildCount(); i++) {
          DefaultMutableTreeNode each = (DefaultMutableTreeNode)node.getChildAt(i);
          if (myUnbuiltNodes.contains(each)) {
            makeLoadingOrLeafIfNoChildren(each);
            childrenToUpdate.add(each);
          }
        }

        if (!childrenToUpdate.isEmpty()) {
          for (DefaultMutableTreeNode each : childrenToUpdate) {
            maybeUpdateSubtreeToUpdate(each);
          }
        }
      }
      else {
        getBuilder().expandNodeChildren(node);
      }

      processSmartExpand(node, canSmartExpand(node, true), false);
      processNodeActionsIfReady(node);
    }

    @Override
    public void treeCollapsed(@NotNull TreeExpansionEvent e) {
      dropUpdaterStateIfExternalChange();

      final TreePath path = e.getPath();
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      NodeDescriptor2 descriptor = getDescriptorFrom(node);
      if (descriptor == null) return;

      TreePath pathToSelect = null;
      if (isSelectionInside(node)) {
        pathToSelect = new TreePath(node.getPath());
      }

      if (true) {//isDisposeOnCollapsing
        runDone(new TreeRunnable2("AbstractTreeUi.MyExpansionListener.treeCollapsed") {
          @Override
          public void perform() {
            if (isDisposed(node)) return;

            TreePath nodePath = new TreePath(node.getPath());
            if (myTree.isExpanded(nodePath)) return;

            removeChildren(node);
            makeLoadingOrLeafIfNoChildren(node);
          }
        });
        if (node.equals(getRootNode())) {
          if (myTree.isRootVisible()) {
            //todo kirillk to investigate -- should be done by standard selction move
            //addSelectionPath(new TreePath(getRootNode().getPath()), true, Condition.FALSE);
          }
        }
        else {
          myTreeModel.reload(node);
        }
      }

      if (pathToSelect != null && myTree.isSelectionEmpty()) {
        addSelectionPath(pathToSelect, true, Conditions.alwaysFalse(), null);
      }
    }
  }

  private void removeChildren(@NotNull DefaultMutableTreeNode node) {
    @SuppressWarnings({"unchecked", "rawtypes"})
    Enumeration<DefaultMutableTreeNode> children = (Enumeration)node.children();
    for (DefaultMutableTreeNode child : Collections.list(children)) {
      disposeNode(child);
    }
    node.removeAllChildren();
    nodeStructureChanged(node);
  }

  private void maybeUpdateSubtreeToUpdate(@NotNull final DefaultMutableTreeNode subtreeRoot) {
    if (!myUnbuiltNodes.contains(subtreeRoot)) return;
    TreePath path = getPathFor(subtreeRoot);

    if (myTree.getRowForPath(path) == -1) return;

    DefaultMutableTreeNode parent = getParentBuiltNode(subtreeRoot);
    if (parent == null) {
      if (!getBuilder().isAlwaysShowPlus(getDescriptorFrom(subtreeRoot))) {
        addSubtreeToUpdate(subtreeRoot);
      }
    }
    else if (parent != subtreeRoot) {
      addNodeAction(getElementFor(subtreeRoot), true, parent1 -> maybeUpdateSubtreeToUpdate(subtreeRoot));
    }
  }

  private boolean isSelectionInside(@NotNull DefaultMutableTreeNode parent) {
    TreePath path = new TreePath(myTreeModel.getPathToRoot(parent));
    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) return false;
    for (TreePath path1 : paths) {
      if (path.isDescendant(path1)) return true;
    }
    return false;
  }

  boolean isInStructure(@Nullable Object element) {
    if (isNodeNull(element)) return false;
    final AbstractTreeStructure structure = getTreeStructure();
    if (structure == null) return false;

    final Object rootElement = structure.getRootElement();
    Object eachParent = element;
    while (eachParent != null) {
      if (Comparing.equal(rootElement, eachParent)) return true;
      eachParent = structure.getParentElement(eachParent);
    }

    return false;
  }

  @FunctionalInterface
  interface NodeAction {
    void onReady(@NotNull DefaultMutableTreeNode node);
  }

  void setCanYield(final boolean canYield) {
    myCanYield = canYield;
  }

  @NotNull
  Collection<TreeUpdatePass2> getYeildingPasses() {
    return myYieldingPasses;
  }

  private static class LoadedChildren {
    @NotNull private final List<Object> myElements;
    private final Map<Object, AbstractTreeNod2> myDescriptors = new HashMap<>();
    private final Map<AbstractTreeNod2, Boolean> myChanges = new HashMap<>();

    LoadedChildren(Object /*@Nullable*/ [] elements) {
      myElements = Arrays.asList(elements != null ? elements : ArrayUtilRt.EMPTY_OBJECT_ARRAY);
    }

    void putDescriptor(Object element, AbstractTreeNod2 descriptor, boolean isChanged) {
      if (isUnitTestingMode()) {
        assert myElements.contains(element);
      }
      myDescriptors.put(element, descriptor);
      myChanges.put(descriptor, isChanged);
    }

    @NotNull
    List<Object> getElements() {
      return myElements;
    }

    AbstractTreeNod2 getDescriptor(Object element) {
      return myDescriptors.get(element);
    }

    @NotNull
    @Override
    public String toString() {
      return myElements + "->" + myChanges;
    }

    public boolean isUpdated(Object element) {
      NodeDescriptor2 desc = getDescriptor(element);
      return myChanges.get(desc);
    }
  }

  private long getComparatorStamp() {
    return myOwnComparatorStamp;
  }

  void incComparatorStamp() {
    myOwnComparatorStamp = getComparatorStamp() + 1;
  }

  private static class UpdateInfo {
    AbstractTreeNod2 myDescriptor;
    TreeUpdatePass2 myPass;
    boolean myCanSmartExpand;
    boolean myWasExpanded;
    boolean myForceUpdate;
    boolean myDescriptorIsUpToDate;
    final boolean myUpdateChildren;

    UpdateInfo(AbstractTreeNod2 descriptor,
               TreeUpdatePass2 pass,
                      boolean canSmartExpand,
                      boolean wasExpanded,
                      boolean forceUpdate,
                      boolean descriptorIsUpToDate,
                      boolean updateChildren) {
      myDescriptor = descriptor;
      myPass = pass;
      myCanSmartExpand = canSmartExpand;
      myWasExpanded = wasExpanded;
      myForceUpdate = forceUpdate;
      myDescriptorIsUpToDate = descriptorIsUpToDate;
      myUpdateChildren = updateChildren;
    }

    synchronized AbstractTreeNod2 getDescriptor() {
      return myDescriptor;
    }

    synchronized TreeUpdatePass2 getPass() {
      return myPass;
    }

    synchronized boolean isCanSmartExpand() {
      return myCanSmartExpand;
    }

    synchronized boolean isWasExpanded() {
      return myWasExpanded;
    }

    synchronized boolean isForceUpdate() {
      return myForceUpdate;
    }

    synchronized boolean isDescriptorIsUpToDate() {
      return myDescriptorIsUpToDate;
    }

    public synchronized void apply(@NotNull UpdateInfo updateInfo) {
      myDescriptor = updateInfo.myDescriptor;
      myPass = updateInfo.myPass;
      myCanSmartExpand = updateInfo.myCanSmartExpand;
      myWasExpanded = updateInfo.myWasExpanded;
      myForceUpdate = updateInfo.myForceUpdate;
      myDescriptorIsUpToDate = updateInfo.myDescriptorIsUpToDate;
    }

    public synchronized  boolean isUpdateChildren() {
      return myUpdateChildren;
    }

    @Override
    @NotNull
    @NonNls
    public synchronized String toString() {
      return "UpdateInfo: desc=" +
             myDescriptor +
             " pass=" +
             myPass +
             " canSmartExpand=" +
             myCanSmartExpand +
             " wasExpanded=" +
             myWasExpanded +
             " forceUpdate=" +
             myForceUpdate +
             " descriptorUpToDate=" +
             myDescriptorIsUpToDate;
    }
  }


  void setPassthroughMode(boolean passthrough) {
    myPassThroughMode = passthrough;
    AbstractTreeUpdater2 updater = getUpdater();

    if (updater != null) {
      updater.setPassThroughMode(myPassThroughMode);
    }

    if (!isUnitTestingMode() && passthrough) {
      // TODO: this assertion should be restored back as soon as possible [JamTreeTableView should be rewritten, etc]
      //LOG.error("Pass-through mode for TreeUi is allowed only for unit test mode");
    }
  }

  boolean isPassthroughMode() {
    return myPassThroughMode;
  }

  private static boolean isUnitTestingMode() {
    Application app = ApplicationManager.getApplication();
    return app != null && app.isUnitTestMode();
  }

  private void addModelListenerToDiagnoseAccessOutsideEdt() {
    myTreeModel.addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
        assertIsDispatchThread();
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
        assertIsDispatchThread();
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
        assertIsDispatchThread();
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        assertIsDispatchThread();
      }
    });
  }

  private <V> void warnMap(String prefix, Map<Object, V> map) {
    if (!LOG.isDebugEnabled()) return;
    if (!SwingUtilities.isEventDispatchThread() && !myPassThroughMode) {
      LOG.warn(prefix + "modified on wrong thread");
    }
    long count = map.keySet().stream().filter(AbstractTreeUi2::isNodeNull).count();
    if (count > 0) LOG.warn(prefix + "null keys: " + count + " / " + map.size());
  }

  /**
   * @param element an element in the tree structure
   * @return {@code true} if element is {@code null} or if it contains a {@code null} value
   */
  private static boolean isNodeNull(Object element) {
    if (element instanceof AbstractTreeNod2) {
      AbstractTreeNod2 node = (AbstractTreeNod2)element;
      element = node.getValue();
    }
    return element == null;
  }

  abstract static class TreeConsumer2<T> extends TreeRunnable2 implements java.util.function.Consumer<T> {
    TreeConsumer2(@NotNull String name) {
      super(name);
    }

    @Override
    public final void accept(T t) {
      run();
    }
  }

}
