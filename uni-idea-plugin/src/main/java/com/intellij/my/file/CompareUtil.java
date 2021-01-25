package com.intellij.my.file;

import static com.intellij.openapi.util.text.StringUtil.naturalCompare;

public class CompareUtil {
  public static int compare(Comparable<?> key1, Comparable<?> key2) {
    if (key1 == null && key2 == null) return 0;
    if (key1 == null) return 1;
    if (key2 == null) return -1;
    if (key1 instanceof String && key2 instanceof String) {
      return naturalCompare((String)key1, (String)key2);
    }

    try {
      //noinspection unchecked,rawtypes
      return ((Comparable)key1).compareTo(key2);
    }
    catch (ClassCastException ignored) {
      // if custom nodes provide comparable keys of different types,
      // let's try to compare class names instead to avoid broken trees
      return key1.getClass().getName().compareTo(key2.getClass().getName());
    }
  }
}
