// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file;

import com.intellij.ide.dnd.DnDAware;
import com.intellij.ide.dnd.TransferableList;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.tree.WideSelectionTreeUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;

public class DnDAwareTree2 extends Tree2 implements DnDAware {

  public DnDAwareTree2(final TreeModel treemodel) {
    super(treemodel);
    initDnD();
  }

  @Override
  public Color getFileColorFor(Object object) {
    return JBColor.RED;
  }

  @Override
  public void processMouseEvent(final MouseEvent e) {
//todo [kirillk] to delegate this to DnDEnabler
    if (getToolTipText() == null && e.getID() == MouseEvent.MOUSE_ENTERED) return;
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (SystemInfo.isMac && SwingUtilities.isRightMouseButton(e) && e.getID() == MouseEvent.MOUSE_DRAGGED) return;
    super.processMouseMotionEvent(e);
  }

  @Override
  public final boolean isOverSelection(final Point point) {
    final TreePath path = WideSelectionTreeUI.isWideSelection(this)
                          ? getClosestPathForLocation(point.x, point.y) : getPathForLocation(point.x, point.y);
    if (path == null) return false;
    return isPathSelected(path);
  }

  @Override
  public void dropSelectionButUnderPoint(final Point point) {
    TreeUtil.dropSelectionButUnderPoint(this, point);
  }

  @Override
  @NotNull
  public final JComponent getComponent() {
    return this;
  }

  private void initDnD() {
    if (!GraphicsEnvironment.isHeadless()) {
      setDragEnabled(true);
      setTransferHandler(DEFAULT_TRANSFER_HANDLER);
    }
  }

  private static final TransferHandler DEFAULT_TRANSFER_HANDLER = new TransferHandler() {
    @Override
    protected Transferable createTransferable(JComponent component) {
      if (component instanceof JTree) {
        JTree tree = (JTree)component;
        TreePath[] selection = tree.getSelectionPaths();
        if (selection != null && selection.length > 1) {
          return new TransferableList<>(selection) {
            @Override
            protected String toString(TreePath path) {
              return String.valueOf(path.getLastPathComponent());
            }
          };
        }
      }
      return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }
  };
}
