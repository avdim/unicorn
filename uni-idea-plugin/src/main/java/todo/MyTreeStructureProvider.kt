package todo

import com.intellij.ide.TreeStructureProvider2
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode2
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import ru.tutu.idea.file.uniFilesRootNodes

class MyTreeStructureProvider : TreeStructureProvider2 {

  override fun modify(
    abstractTreeNode: AbstractTreeNod2<*>,
    collection: Collection<AbstractTreeNod2<*>?>,
    viewSettings: ViewSettings
  ): Collection<AbstractTreeNod2<*>> {
//    val project: Project? = if (abstractTreeNode is com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode2) abstractTreeNode.getProject() else null
//    if (project != null && com.unicorn.Uni.USE_FILE_TREE_PROVIDER) {
//      return collection.filterNotNull() + uniFilesRootNodes(project, viewSettings) //+ ProjectViewNode()
//    } else {
      return collection.filterNotNull()
//    }
  }

  override fun getData(
    selected: Collection<AbstractTreeNod2<*>?>,
    dataId: String
  ): Any? {
//    if (false && selected.any { it is TutuProjectViewProjectNode }) {
//      return LocalFileSystem.getInstance().findFileByIoFile(File(ConfUnitFiles.ROOT_PATH))
//    } else {
      return null
//    }

  }
}
