/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.util.treeView;

import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashSet;
import java.util.Set;

public class TreeUpdatePass2 {
  private final DefaultMutableTreeNode myNode;

  private long myUpdateStamp;
  private boolean myExpired;

  private DefaultMutableTreeNode myCurrentNode;

  private final long myAllocation;

  private boolean myUpdateChildren = true;
  private boolean myUpdateStructure = true;
  private final Set<AbstractTreeNod2> myUpdatedDescriptors = new HashSet<>();

  public TreeUpdatePass2(@NotNull final DefaultMutableTreeNode node) {
    myNode = node;
    myAllocation = System.currentTimeMillis();
  }

  public TreeUpdatePass2 setUpdateChildren(boolean updateChildren) {
    myUpdateChildren = updateChildren;
    return this;
  }

  public boolean isUpdateChildren() {
    return myUpdateChildren;
  }

  @NotNull
  public DefaultMutableTreeNode getNode() {
    return myNode;
  }

  public TreeUpdatePass2 setUpdateStamp(final long updateCount) {
    myUpdateStamp = updateCount;
    return this;
  }

  public long getUpdateStamp() {
    return myUpdateStamp;
  }

  public void expire() {
    myExpired = true;
  }

  public boolean isExpired() {
    return myExpired;
  }

  public DefaultMutableTreeNode getCurrentNode() {
    return myCurrentNode;
  }

  public void setCurrentNode(DefaultMutableTreeNode currentNode) {
    myCurrentNode = currentNode;
  }

  @NonNls
  @Override
  public String toString() {
    return "TreeUpdatePass node=" + myNode + " structure=" + myUpdateStructure + " stamp=" + myUpdateStamp + " expired=" + myExpired + " currentNode=" + myCurrentNode + " allocation=" + myAllocation;
  }

  public boolean willUpdate(@NotNull DefaultMutableTreeNode node) {
    @NotNull DefaultMutableTreeNode currentNode = myCurrentNode != null ? myCurrentNode : myNode;
    return node.isNodeAncestor(currentNode);
  }

  public TreeUpdatePass2 setUpdateStructure(boolean update) {
    myUpdateStructure = update;
    return this;
  }

  public boolean isUpdateStructure() {
    return myUpdateStructure;
  }

  public void addToUpdated(AbstractTreeNod2 nodeDescriptor) {
    myUpdatedDescriptors.add(nodeDescriptor);
  }

  public boolean isUpdated(AbstractTreeNod2 descriptor) {
    return myUpdatedDescriptors.contains(descriptor);
  }
}