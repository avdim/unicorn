package todo

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import ru.tutu.idea.file.uniFilesRootNodes

class MyTreeStructureProvider : TreeStructureProvider {

  override fun modify(
    abstractTreeNode: AbstractTreeNode<*>,
    collection: Collection<AbstractTreeNode<*>?>,
    viewSettings: ViewSettings
  ): Collection<AbstractTreeNode<*>> {
    val project: Project? = if (abstractTreeNode is ProjectViewProjectNode) abstractTreeNode.getProject() else null
    if (project != null && com.unicorn.Uni.USE_FILE_TREE_PROVIDER) {
      return collection.filterNotNull() + uniFilesRootNodes(project, viewSettings) //+ ProjectViewNode()
    } else {
      return collection.filterNotNull()
    }
  }

  override fun getData(
    selected: Collection<AbstractTreeNode<*>?>,
    dataId: String
  ): Any? {
//    if (false && selected.any { it is TutuProjectViewProjectNode }) {
//      return LocalFileSystem.getInstance().findFileByIoFile(File(ConfUnitFiles.ROOT_PATH))
//    } else {
      return null
//    }

  }
}
