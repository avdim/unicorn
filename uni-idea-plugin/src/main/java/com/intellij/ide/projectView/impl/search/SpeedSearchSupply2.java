/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectView.impl.search;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * @author spLeaner
 * @author Konstantin Bulenkov
 */
public abstract class SpeedSearchSupply2 {
  private static final Key SPEED_SEARCH_COMPONENT_MARKER = new Key("SPEED_SEARCH_COMPONENT_MARKER");
  public static final String ENTERED_PREFIX_PROPERTY_NAME = "enteredPrefix";

  public abstract boolean isPopupActive();

  @Nullable
  public String getEnteredPrefix() {
    return null;
  }

  protected void installSupplyTo(@NotNull JComponent component) {
    component.putClientProperty(SPEED_SEARCH_COMPONENT_MARKER, this);
    addChangeListener(evt -> component.repaint());
  }

  public abstract void addChangeListener(@NotNull PropertyChangeListener listener);

  /**
   * Find an element matching the searching query in the underlying component and select it there. Speed-search popup is not affected.
   * @param searchQuery text that the selected element should match
   */
  public abstract void findAndSelectElement(@NotNull String searchQuery);

}
