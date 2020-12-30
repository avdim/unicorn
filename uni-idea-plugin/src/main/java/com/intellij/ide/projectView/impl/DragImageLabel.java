package com.intellij.ide.projectView.impl;

import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DragImageLabel extends JLabel {

  public DragImageLabel(Project myProject, JTree tree, @Nls String text, Icon icon, @Nullable VirtualFile file) {
    super(text, icon, SwingConstants.LEADING);
    setFont(UIUtil.getTreeFont());
    setOpaque(true);
    if (file != null) {
      setBackground(EditorTabPresentationUtil.getEditorTabBackgroundColor(myProject, file, null));
      setForeground(EditorTabPresentationUtil.getFileForegroundColor(myProject, file));
    } else {
      setForeground(RenderingUtil.getForeground(tree, true));
      setBackground(RenderingUtil.getBackground(tree, true));
    }
    setBorder(new EmptyBorder(JBUI.CurrentTheme.EditorTabs.tabInsets()));
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    size.height = JBUI.scale(SingleHeightTabs.UNSCALED_PREF_HEIGHT);
    return size;
  }
}
