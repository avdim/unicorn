// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file;

import com.intellij.ide.util.treeView.FileNameComparator;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;

public final class AlphaComparator2 {

  public static int compare(AbstractTreeNod2 nodeDescriptor1, AbstractTreeNod2 AbstractTreeNod2) {
    int weight1 = nodeDescriptor1.getWeight();
    int weight2 = AbstractTreeNod2.getWeight();
    if (weight1 != weight2) {
      return weight1 - weight2;
    }
    String s1 = nodeDescriptor1.toString();
    String s2 = AbstractTreeNod2.toString();
    if (s1 == null) return s2 == null ? 0 : -1;
    if (s2 == null) return +1;

    return FileNameComparator.INSTANCE.compare(s1, s2);
  }
}
