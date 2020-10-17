package todo.mvi

import kotlinx.coroutines.CoroutineScope
import com.unicorn.plugin.mvi.Column
import com.unicorn.plugin.mvi.UniWindowState
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class FileManagerMviStore(
  val stateFlow: StateFlow<UniWindowState>,
  val intent: suspend (Intent) -> Unit
)

fun CoroutineScope.createFileManagerMviStore(): Store<UniWindowState, Intent> {
  val mviStore = createStore(
    UniWindowState(
      columns = listOf(
        Column(paths = ConfUniFiles.DEFAULT_PATHS),
        Column(paths = listOf("/"))
      )
    )
  ) { s, a:Intent->
    reduce(s, a)
  }
  return mviStore

}
