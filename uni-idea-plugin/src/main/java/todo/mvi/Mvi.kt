package todo.mvi

import com.unicorn.plugin.mvi.Column
import com.unicorn.plugin.mvi.UniWindowState
import ru.avdim.mvi.Store
import ru.avdim.mvi.createStore
import ru.tutu.idea.file.ConfUniFiles

inline fun <T> Collection<T>.transformI(index: Int, lambda: (T) -> T): List<T> =
  mapIndexed { i, element ->
    if (i == index) {
      lambda(element)
    } else {
      element
    }
  }

fun createFileManagerMviStore(): Store<UniWindowState, Intent> {
  val mviStore = createStore(
    UniWindowState(
      columns = listOf(
        Column(paths = ConfUniFiles.DEFAULT_PATHS.map { it.absolutePath }),
        Column(paths = listOf("/"))
      )
    )
  ) { s, a:Intent->
    when (a) {
      is Intent.EditPath -> s.copy(
        columns = s.columns.transformI(a.col) {
          it.copy(paths = it.paths.transformI(a.row) { a.path })
        }
      )
      is Intent.AddPath -> s.copy(
        columns = s.columns.transformI(a.col) {
          it.copy(paths = it.paths + ConfUniFiles.DEFAULT_NEW_PATH)
        }
      )
      is Intent.RemovePath -> s.copy(
        columns = s.columns.transformI(a.col) {
          it.copy(paths = it.paths.dropIndex(a.row))
        }
      )
      is Intent.AddColumn -> s.copy(
        columns = s.columns + Column(paths = ConfUniFiles.DEFAULT_PATHS.map { it.absolutePath })
      )
      is Intent.RemoveColumn -> {
        s.copy(
          columns = s.columns.dropIndex(a.col)
        )
      }
      is Intent.RenderFiles -> {
        s.copy(
          renderFiles = true
        )
      }
      is Intent.Update -> {
        s.copy(
          forceUpdate = s.forceUpdate + 1
        )
      }
    }
  }
  return mviStore

}
