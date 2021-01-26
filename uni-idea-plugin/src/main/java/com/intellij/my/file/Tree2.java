// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.treeView.*;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.*;
import com.intellij.ui.tree.TreePathBackgroundSupplier;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.*;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.Position;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

import static com.intellij.ide.dnd.SmoothAutoScroller.installDropTargetAsNecessary;

public class Tree2 extends JTree implements ComponentWithEmptyText, ComponentWithExpandableItems<Integer>, Queryable,
                                           ComponentWithFileColors, TreePathBackgroundSupplier {
  @ApiStatus.Internal
  public static final Key<Boolean> AUTO_SELECT_ON_MOUSE_PRESSED = Key.create("allows to select a node automatically on right click");

  private final StatusText myEmptyText;
  private final ExpandableItemsHandler<Integer> myExpandableItemsHandler;

  private AsyncProcessIcon myBusyIcon;
  private Rectangle myLastVisibleRec;
  private final MySelectionModel mySelectionModel = new MySelectionModel();

  private TreePath rollOverPath;

  public Tree2(TreeModel treemodel) {
    super(treemodel);
    myEmptyText = new StatusText(this) {
      @Override
      protected boolean isStatusVisible() {
        return Tree2.this.isEmpty();
      }
    };

    myExpandableItemsHandler = ExpandableItemsHandlerFactory.install(this);

    if (UIUtil.isUnderWin10LookAndFeel()) {
      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          Point p = e.getPoint();
          TreePath newPath = getPathForLocation(p.x, p.y);
          if (newPath != null && !newPath.equals(rollOverPath)) {
            TreeCellRenderer renderer = getCellRenderer();
            if (newPath.getLastPathComponent() instanceof TreeNode) {
              TreeNode node = (TreeNode)newPath.getLastPathComponent();
              JComponent c = (JComponent)renderer.getTreeCellRendererComponent(
                Tree2.this, node,
                isPathSelected(newPath),
                isExpanded(newPath),
                getModel().isLeaf(node),
                getRowForPath(newPath), hasFocus());

              c.putClientProperty(UIUtil.CHECKBOX_ROLLOVER_PROPERTY, c instanceof JCheckBox ? getPathBounds(newPath) : node);
              rollOverPath = newPath;
              UIUtil.repaintViewport(Tree2.this);
            }
          }
        }
      });
    }

    addMouseListener(new MyMouseListener());
    addFocusListener(new MyFocusListener());

    setCellRenderer(new NodeRenderer());

    setSelectionModel(mySelectionModel);
    setOpaque(false);
  }

  @Override
  public void setUI(TreeUI ui) {
    super.setUI(ui);
  }

  @Override
  protected Graphics getComponentGraphics(Graphics graphics) {
    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
  }

  public boolean isEmpty() {
    return 0 >= getRowCount();
  }

  @Override
  public boolean isFileColorsEnabled() {
    return false;
  }

  @NotNull
  @Override
  public StatusText getEmptyText() {
    return myEmptyText;
  }

  @Override
  @NotNull
  public ExpandableItemsHandler<Integer> getExpandableItemsHandler() {
    return myExpandableItemsHandler;
  }

  @Override
  public void setExpandableItemsEnabled(boolean enabled) {
    myExpandableItemsHandler.setEnabled(enabled);
  }

  @Override
  public Color getBackground() {
    return isBackgroundSet() ? super.getBackground() : UIUtil.getTreeBackground();
  }

  @Override
  public Color getForeground() {
    return isForegroundSet() ? super.getForeground() : UIUtil.getTreeForeground();
  }

  @Override
  public void addNotify() {
    super.addNotify();

    // hack to invalidate sizes, see BasicTreeUI.Handler.propertyChange
    // now the sizes calculated before the tree has the correct GraphicsConfiguration and may be incorrect on the secondary display
    // see IDEA-184010
    firePropertyChange("font", null, null);

    updateBusy();
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    if (myBusyIcon != null) {
      remove(myBusyIcon);
      Disposer.dispose(myBusyIcon);
      myBusyIcon = null;
    }
  }

  @Override
  public void doLayout() {
    super.doLayout();

    updateBusyIconLocation();
  }

  private void updateBusyIconLocation() {
    if (myBusyIcon != null) {
      myBusyIcon.updateLocation(this);
    }
  }

  @Override
  public void paint(Graphics g) {
    Rectangle visible = getVisibleRect();

    if (!AbstractTreeBuilder2.isToPaintSelection(this)) {
      boolean canHoldSelection = false;
      TreePath[] paths = getSelectionModel().getSelectionPaths();
      if (paths != null) {
        for (TreePath each : paths) {
          Rectangle selection = getPathBounds(each);
          if (selection != null && (g.getClipBounds().intersects(selection) || g.getClipBounds().contains(selection))) {
            canHoldSelection = true;
            break;
          }
        }
      }

      if (canHoldSelection) {
        mySelectionModel.holdSelection();
      }
    }

    try {
      super.paint(g);

      if (!visible.equals(myLastVisibleRec)) {
        updateBusyIconLocation();
      }

      myLastVisibleRec = visible;
    }
    finally {
      mySelectionModel.unholdSelection();
    }
  }

  private void updateBusy() {
    if (myBusyIcon != null) {
      myBusyIcon.suspend();
      myBusyIcon.setToolTipText(null);
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        if (myBusyIcon != null) {
          repaint();
        }
      });
      updateBusyIconLocation();
    }
  }

  protected boolean shouldShowBusyIconIfNeeded() {
    // https://youtrack.jetbrains.com/issue/IDEA-101422 "Rotating wait symbol in Project list whenever typing"
    return hasFocus();
  }

  protected boolean paintNodes() {
    return false;
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (paintNodes()) {
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());

      paintNodeContent(g);
    }

    if (isFileColorsEnabled()) {
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());

      paintFileColorGutter(g);
    }

    super.paintComponent(g);
    myEmptyText.paint(this, g);
  }

  protected void paintFileColorGutter(Graphics g) {
    GraphicsConfig config = new GraphicsConfig(g);
    Rectangle rect = getVisibleRect();
    int firstVisibleRow = getClosestRowForLocation(rect.x, rect.y);
    int lastVisibleRow = getClosestRowForLocation(rect.x, rect.y + rect.height);

    for (int row = firstVisibleRow; row <= lastVisibleRow; row++) {
      TreePath path = getPathForRow(row);
      Color color = path == null ? null : getFileColorForPath(path);
      if (color != null) {
        Rectangle bounds = getRowBounds(row);
        g.setColor(color);
        g.fillRect(0, bounds.y, getWidth(), bounds.height);
      }
    }
    config.restore();
  }

  @Override
  @Nullable
  public Color getPathBackground(@NotNull TreePath path, int row) {
    return isFileColorsEnabled() ? getFileColorForPath(path) : null;
  }

  @Nullable
  public Color getFileColorForPath(@NotNull TreePath path) {
    Object component = path.getLastPathComponent();
    if (component instanceof LoadingNode) {
      Object[] pathObjects = path.getPath();
      if (pathObjects.length > 1) {
        component = pathObjects[pathObjects.length - 2];
      }
    }
    return getFileColorFor(TreeUtil.getUserObject(component));
  }

  @Nullable
  public Color getFileColorFor(Object object) {
    return null;
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    super.processKeyEvent(e);
  }

  /**
   * Hack to prevent loosing multiple selection on Mac when clicking Ctrl+Left Mouse Button.
   * See faulty code at BasicTreeUI.selectPathForEvent():2245
   * <p>
   * Another hack to match selection UI (wide) and selection behavior (narrow) in Nimbus/GTK+.
   */
  @Override
  protected void processMouseEvent(MouseEvent e) {
    MouseEvent e2 = e;

    if (SystemInfo.isMac) {
      e2 = MacUIUtil.fixMacContextMenuIssue(e);
    }

    super.processMouseEvent(e2);

    if (e != e2 && e2.isConsumed()) e.consume();
  }

  /**
   * Disable Sun's speed search
   */
  @Override
  public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
    return null;
  }

  protected boolean highlightSingleNode() {
    return true;
  }

  private void paintNodeContent(Graphics g) {
    if (!(getUI() instanceof BasicTreeUI)) return;

    AbstractTreeBuilder2 builder = AbstractTreeBuilder2.getBuilderFor(this);
    if (builder == null || builder.isDisposed()) return;

    GraphicsConfig config = new GraphicsConfig(g);
    config.setAntialiasing(true);

    AbstractTreeStructure structure = builder.getTreeStructure();

    for (int eachRow = 0; eachRow < getRowCount(); eachRow++) {
      TreePath path = getPathForRow(eachRow);
      PresentableNodeDescriptor node = toPresentableNode(path.getLastPathComponent());
      if (node == null) continue;

      if (!node.isContentHighlighted()) continue;

      if (highlightSingleNode()) {
        if (node.isContentHighlighted()) {
          TreePath nodePath = getPath(node);

          Rectangle rect;

          Rectangle parentRect = getPathBounds(nodePath);
          if (isExpanded(nodePath)) {
            int[] max = getMax(node, structure);
            rect = new Rectangle(parentRect.x,
                                 parentRect.y,
                                 Math.max((int)parentRect.getMaxX(), max[1]) - parentRect.x - 1,
                                 Math.max((int)parentRect.getMaxY(), max[0]) - parentRect.y - 1);
          }
          else {
            rect = parentRect;
          }

          if (rect != null) {
            Color highlightColor = node.getHighlightColor();
            g.setColor(highlightColor);
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);
            g.setColor(highlightColor.darker());
            g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);
          }
        }
      }
      else {
        //todo: to investigate why it might happen under 1.6: http://www.productiveme.net:8080/browse/PM-217
        if (node.getParentDescriptor() == null) continue;

        Object[] kids = structure.getChildElements(node);
        if (kids.length == 0) continue;

        PresentableNodeDescriptor first = null;
        PresentableNodeDescriptor last = null;
        int lastIndex = -1;
        for (int i = 0; i < kids.length; i++) {
          Object kid = kids[i];
          if (kid instanceof PresentableNodeDescriptor) {
            PresentableNodeDescriptor eachKid = (PresentableNodeDescriptor)kid;
            if (!node.isHighlightableContentNode(eachKid)) continue;
            if (first == null) {
              first = eachKid;
            }
            last = eachKid;
            lastIndex = i;
          }
        }

        if (first == null) continue;
        Rectangle firstBounds = getPathBounds(getPath(first));

        if (isExpanded(getPath(last))) {
          if (lastIndex + 1 < kids.length) {
            Object child = kids[lastIndex + 1];
            if (child instanceof PresentableNodeDescriptor) {
              PresentableNodeDescriptor nextKid = (PresentableNodeDescriptor)child;
              int nextRow = getRowForPath(getPath(nextKid));
              last = toPresentableNode(getPathForRow(nextRow - 1).getLastPathComponent());
            }
          }
          else {
            NodeDescriptor parentNode = node.getParentDescriptor();
            if (parentNode instanceof PresentableNodeDescriptor) {
              PresentableNodeDescriptor ppd = (PresentableNodeDescriptor)parentNode;
              int nodeIndex = node.getIndex();
              if (nodeIndex + 1 < structure.getChildElements(ppd).length) {
                PresentableNodeDescriptor nextChild = ppd.getChildToHighlightAt(nodeIndex + 1);
                int nextRow = getRowForPath(getPath(nextChild));
                TreePath prevPath = getPathForRow(nextRow - 1);
                if (prevPath != null) {
                  last = toPresentableNode(prevPath.getLastPathComponent());
                }
              }
              else {
                int lastRow = getRowForPath(getPath(last));
                PresentableNodeDescriptor lastParent = last;
                boolean lastWasFound = false;
                for (int i = lastRow + 1; i < getRowCount(); i++) {
                  PresentableNodeDescriptor eachNode = toPresentableNode(getPathForRow(i).getLastPathComponent());
                  if (!node.isParentOf(eachNode)) {
                    last = lastParent;
                    lastWasFound = true;
                    break;
                  }
                  lastParent = eachNode;
                }
                if (!lastWasFound) {
                  last = toPresentableNode(getPathForRow(getRowCount() - 1).getLastPathComponent());
                }
              }
            }
          }
        }

        if (last == null) continue;
        Rectangle lastBounds = getPathBounds(getPath(last));

        if (firstBounds == null || lastBounds == null) continue;

        Rectangle toPaint = new Rectangle(firstBounds.x, firstBounds.y, 0, (int)lastBounds.getMaxY() - firstBounds.y - 1);

        toPaint.width = getWidth() - toPaint.x - 4;

        Color highlightColor = first.getHighlightColor();
        g.setColor(highlightColor);
        g.fillRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, 4, 4);
        g.setColor(highlightColor.darker());
        g.drawRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, 4, 4);
      }
    }

    config.restore();
  }

  private int[] getMax(PresentableNodeDescriptor node, AbstractTreeStructure structure) {
    int x = 0;
    int y = 0;
    Object[] children = structure.getChildElements(node);
    for (Object child : children) {
      if (child instanceof PresentableNodeDescriptor) {
        TreePath childPath = getPath((PresentableNodeDescriptor)child);
        if (childPath != null) {
          if (isExpanded(childPath)) {
            int[] tmp = getMax((PresentableNodeDescriptor)child, structure);
            y = Math.max(y, tmp[0]);
            x = Math.max(x, tmp[1]);
          }

          Rectangle r = getPathBounds(childPath);
          if (r != null) {
            y = Math.max(y, (int)r.getMaxY());
            x = Math.max(x, (int)r.getMaxX());
          }
        }
      }
    }

    return new int[]{y, x};
  }

  @Nullable
  private static PresentableNodeDescriptor toPresentableNode(Object pathComponent) {
    if (!(pathComponent instanceof DefaultMutableTreeNode)) return null;
    Object userObject = ((DefaultMutableTreeNode)pathComponent).getUserObject();
    if (!(userObject instanceof PresentableNodeDescriptor)) return null;
    return (PresentableNodeDescriptor)userObject;
  }

  public TreePath getPath(@NotNull PresentableNodeDescriptor node) {
    AbstractTreeBuilder2 builder = AbstractTreeBuilder2.getBuilderFor(this);
    DefaultMutableTreeNode treeNode = builder.getNodeForElement(node);

    return treeNode != null ? new TreePath(treeNode.getPath()) : new TreePath(node);
  }

  @Override
  public void collapsePath(TreePath path) {
    int row = Registry.is("ide.tree.collapse.recursively") ? getRowForPath(path) : -1;
    if (row < 0) {
      super.collapsePath(path);
    }
    else if (!isAlwaysExpanded(path)) {
      ArrayDeque<TreePath> deque = new ArrayDeque<>();
      deque.addFirst(path);
      while (++row < getRowCount()) {
        TreePath next = getPathForRow(row);
        if (!path.isDescendant(next)) break;
        if (isExpanded(next)) deque.addFirst(next);
      }
      deque.forEach(super::collapsePath);
    }
  }

  private boolean isAlwaysExpanded(TreePath path) {
    return path != null && TreeUtil.getNodeDepth(this, path) <= 0;
  }

  private static class MySelectionModel extends DefaultTreeSelectionModel {

    private TreePath[] myHeldSelection;

    @Override
    protected void fireValueChanged(TreeSelectionEvent e) {
      if (myHeldSelection == null) {
        super.fireValueChanged(e);
      }
    }

    public void holdSelection() {
      myHeldSelection = getSelectionPaths();
    }

    public void unholdSelection() {
      if (myHeldSelection != null) {
        setSelectionPaths(myHeldSelection);
        myHeldSelection = null;
      }
    }
  }

  private class MyMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent event) {
      setPressed(event, true);

      if (Boolean.FALSE.equals(UIUtil.getClientProperty(event.getSource(), AUTO_SELECT_ON_MOUSE_PRESSED))) return;
      if (!SwingUtilities.isLeftMouseButton(event) &&
          (SwingUtilities.isRightMouseButton(event) || SwingUtilities.isMiddleMouseButton(event))) {
        TreePath path = getClosestPathForLocation(event.getX(), event.getY());
        if (path == null) return;

        Rectangle bounds = getPathBounds(path);
        if (bounds != null && bounds.y + bounds.height < event.getY()) return;

        if (getSelectionModel().getSelectionMode() != TreeSelectionModel.SINGLE_TREE_SELECTION) {
          TreePath[] selectionPaths = getSelectionModel().getSelectionPaths();
          if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
              if (selectionPath != null && selectionPath.equals(path)) return;
            }
          }
        }
        getSelectionModel().setSelectionPath(path);
      }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
      setPressed(event, false);
      if (event.getButton() == MouseEvent.BUTTON1 &&
          event.getClickCount() == 2 &&
          TreeUtil.isLocationInExpandControl(Tree2.this, event.getX(), event.getY())) {
        event.consume();
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (UIUtil.isUnderWin10LookAndFeel() && rollOverPath != null) {
        TreeCellRenderer renderer = getCellRenderer();
        if (rollOverPath.getLastPathComponent() instanceof TreeNode) {
          TreeNode node = (TreeNode)rollOverPath.getLastPathComponent();
          JComponent c = (JComponent)renderer.getTreeCellRendererComponent(
            Tree2.this, node,
            isPathSelected(rollOverPath),
            isExpanded(rollOverPath),
            getModel().isLeaf(node),
            getRowForPath(rollOverPath), hasFocus());

          c.putClientProperty(UIUtil.CHECKBOX_ROLLOVER_PROPERTY, null);
          rollOverPath = null;
          UIUtil.repaintViewport(Tree2.this);
        }
      }
    }

    private void setPressed(MouseEvent e, boolean pressed) {
      if (UIUtil.isUnderWin10LookAndFeel()) {
        Point p = e.getPoint();
        TreePath path = getPathForLocation(p.x, p.y);
        if (path != null) {
          if (path.getLastPathComponent() instanceof TreeNode) {
            TreeNode node = (TreeNode)path.getLastPathComponent();
            JComponent c = (JComponent)getCellRenderer().getTreeCellRendererComponent(
              Tree2.this, node,
              isPathSelected(path), isExpanded(path),
              getModel().isLeaf(node),
              getRowForPath(path), hasFocus());
            if (pressed) {
              c.putClientProperty(UIUtil.CHECKBOX_PRESSED_PROPERTY, c instanceof JCheckBox ? getPathBounds(path) : node);
            }
            else {
              c.putClientProperty(UIUtil.CHECKBOX_PRESSED_PROPERTY, null);
            }
            UIUtil.repaintViewport(Tree2.this);
          }
        }
      }
    }
  }

  /**
   * This is patch for 4893787 SUN bug. The problem is that the BasicTreeUI.FocusHandler repaints
   * only lead selection index on focus changes. It's a problem with multiple selected nodes.
   */
  private class MyFocusListener extends FocusAdapter {
    private void focusChanges() {
      TreePath[] paths = getSelectionPaths();
      if (paths != null) {
        TreeUI ui = getUI();
        for (int i = paths.length - 1; i >= 0; i--) {
          Rectangle bounds = ui.getPathBounds(Tree2.this, paths[i]);
          if (bounds != null) {
            repaint(bounds);
          }
        }
      }
    }

    @Override
    public void focusGained(FocusEvent e) {
      focusChanges();
    }

    @Override
    public void focusLost(FocusEvent e) {
      focusChanges();
    }
  }

  /**
   * @deprecated no effect
   */
  @Deprecated
  public final void setLineStyleAngled() {
  }

  public interface NodeFilter<T> {
    boolean accept(T node);
  }

  @Override
  public void putInfo(@NotNull Map<String, String> info) {
    TreePath[] selection = getSelectionPaths();
    if (selection == null) return;

    StringBuilder nodesText = new StringBuilder();

    for (TreePath eachPath : selection) {
      Object eachNode = eachPath.getLastPathComponent();
      Component c =
        getCellRenderer().getTreeCellRendererComponent(this, eachNode, false, false, false, getRowForPath(eachPath), false);

      if (c != null) {
        if (nodesText.length() > 0) {
          nodesText.append(";");
        }
        nodesText.append(c);
      }
    }

    if (nodesText.length() > 0) {
      info.put("selectedNodes", nodesText.toString());
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return super.getPreferredSize();
  }

  @Override
  public void scrollPathToVisible(@Nullable TreePath path) {
    if (path == null) return; // nothing to scroll
    makeVisible(path); // expand parent paths if needed
    TreeUtil.scrollToVisible(this, path, false);
  }

  @Override
  public void setTransferHandler(TransferHandler handler) {
    installDropTargetAsNecessary(this);
    super.setTransferHandler(handler);
  }
}
