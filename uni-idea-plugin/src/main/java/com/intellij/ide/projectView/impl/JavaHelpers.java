package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.RootsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.ide.projectView.impl.HelpersKt.getValueFromNode;

public class JavaHelpers {
  @NotNull
  public static Object[] pathsToSelectedElements(TreePath[] paths) {
    if (paths == null) return PsiElement.EMPTY_ARRAY;
    ArrayList<Object> list = new ArrayList<>(paths.length);
    for (TreePath path : paths) {
      Object lastPathComponent = path.getLastPathComponent();
      Object element = getValueFromNode(lastPathComponent);
      if (element instanceof Object[]) {
        Collections.addAll(list, (Object[])element);
      }
      else if (element != null) {
        list.add(element);
      }
    }
    return ArrayUtil.toObjectArray(list);
  }

  @NotNull
  public static List<PsiElement> getElementsFromNode(@NotNull Project project, @Nullable Object node) {
    Object value = getValueFromNode(node);
    JBIterable<?> it = value instanceof PsiElement || value instanceof VirtualFile ? JBIterable.of(value) :
      value instanceof Object[] ? JBIterable.of((Object[])value) :
        value instanceof Iterable ? JBIterable.from((Iterable<?>)value) :
          JBIterable.of(TreeUtil.getUserObject(node));
    return it.flatten(o -> o instanceof RootsProvider ? ((RootsProvider)o).getRoots() : Collections.singleton(o))
      .map(o -> o instanceof VirtualFile ? PsiUtilCore.findFileSystemItem(project, (VirtualFile)o) : o)
      .filter(PsiElement.class)
      .filter(PsiElement::isValid)
      .toList();
  }

}
