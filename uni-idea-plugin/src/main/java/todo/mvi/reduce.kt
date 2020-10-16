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

inline fun <reified T> Iterable<T>.dropIndex(index: Int) =
  filterIndexed { i, _ -> i != index }

