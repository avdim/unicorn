package todo.mvi

import ru.tutu.idea.file.ConfUniFiles
import com.unicorn.plugin.mvi.Column
import com.unicorn.plugin.mvi.UniWindowState

sealed class Intent {
  class EditPath(val col: Int, val row: Int, val path: String) : Intent()
  class AddPath(val col: Int) : Intent()
  class RemovePath(val col: Int, val row: Int) : Intent()
  class AddColumn() : Intent()
  class RemoveColumn(val col: Int) : Intent()
  object RenderFiles : Intent()
  object Update : Intent()
}

fun reduce(state: UniWindowState, intent: Intent): UniWindowState = when (intent) {
  is Intent.EditPath -> state.copy(
    columns = state.columns.transformI(intent.col) {
      it.copy(paths = it.paths.transformI(intent.row) { intent.path })
    }
  )
  is Intent.AddPath -> state.copy(
    columns = state.columns.transformI(intent.col) {
      it.copy(paths = it.paths + ConfUniFiles.DEFAULT_NEW_PATH)
    }
  )
  is Intent.RemovePath -> state.copy(
    columns = state.columns.transformI(intent.col) {
      it.copy(paths = it.paths.dropIndex(intent.row))
    }
  )
  is Intent.AddColumn -> state.copy(
    columns = state.columns + Column(paths = ConfUniFiles.DEFAULT_PATHS)
  )
  is Intent.RemoveColumn -> {
    state.copy(
      columns = state.columns.dropIndex(intent.col)
    )
  }
  is Intent.RenderFiles -> {
    state.copy(
      renderFiles = true
    )
  }
  is Intent.Update -> {
    state.copy(
      forceUpdate = state.forceUpdate + 1
    )
  }
}

inline fun <reified T> Iterable<T>.dropIndex(index: Int) =
  filterIndexed { i, _ -> i != index }

