package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TreeSpeedSearch;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.StringTokenizer;

public final class SpeedSearchFiles extends TreeSpeedSearch {
  public SpeedSearchFiles(JTree tree) {
    super(tree);
  }

  @Override
  protected boolean isMatchingElement(Object element, String pattern) {
    Object userObject = ((DefaultMutableTreeNode) ((TreePath) element).getLastPathComponent()).getUserObject();
    if (userObject instanceof PsiDirectoryNode) {
      String str = getElementText(element);
      if (str == null) return false;
      str = StringUtil.toLowerCase(str);
      if (pattern.indexOf('.') >= 0) {
        return compare(str, pattern);
      }
      StringTokenizer tokenizer = new StringTokenizer(str, ".");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        if (compare(token, pattern)) {
          return true;
        }
      }
      return false;
    } else {
      return super.isMatchingElement(element, pattern);
    }
  }
}
