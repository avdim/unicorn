// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file;

import com.intellij.ide.util.treeView.FileNameComparator;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import org.jetbrains.annotations.NotNull;

public final class AlphaComparator2 {

  public static int compare(@NotNull AbstractTreeNod2 a,@NotNull AbstractTreeNod2 b) {
    int weight1 = a.getWeight();
    int weight2 = b.getWeight();
    if (weight1 != weight2) {
      return weight1 - weight2;
    }
    @NotNull String s1 = a.getSortedName();
    @NotNull String s2 = b.getSortedName();
    return FileNameComparator.INSTANCE.compare(s1, s2);
  }
}
