/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.unicorn.plugin;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Avdeev
 */
public class TestActionEvent2 extends AnActionEvent {

  public boolean IsFromActionToolbar;
  public boolean IsFromContextMenu;

  public TestActionEvent2(@NotNull DataContext dataContext,
                          @NotNull AnAction action) {
    super(null, dataContext, "", action.getTemplatePresentation().clone(), ActionManager.getInstance(), 0);
  }

  public TestActionEvent2(@NotNull AnAction action) {
    this(DataManager.getInstance().getDataContext(), action);
  }

  public TestActionEvent2(Presentation presentation) {
    super(null, DataManager.getInstance().getDataContext(), "", presentation, ActionManager.getInstance(), 0);
  }

  public TestActionEvent2(DataContext context) {
    super(null, context, "", new Presentation(), ActionManager.getInstance(), 0);
  }

  public TestActionEvent2() {
    super(null, DataManager.getInstance().getDataContext(), "", new Presentation(), ActionManager.getInstance(), 0);
  }

  @Override
  public boolean isFromActionToolbar() {
    return IsFromActionToolbar;
  }

  @Override
  public boolean isFromContextMenu() {
    return IsFromContextMenu;
  }
}
