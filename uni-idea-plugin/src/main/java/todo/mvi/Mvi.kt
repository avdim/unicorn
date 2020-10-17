package todo.mvi

import kotlinx.coroutines.CoroutineScope
import com.unicorn.plugin.mvi.Column
import com.unicorn.plugin.mvi.UniWindowState
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

//todo migrate to mvi store
fun CoroutineScope.createFileManagerMviStore(): FileManagerMviStore {
  val mutableStateFlow = MutableStateFlow(
    UniWindowState(
      columns = listOf(
        Column(paths = ConfUniFiles.DEFAULT_PATHS),
        Column(paths = listOf("/"))
      )
    )
  )
  val intents = actor<Intent> {
    channel.consumeEach {
      mutableStateFlow.value = reduce(mutableStateFlow.value, it)
    }
  }
  return FileManagerMviStore(mutableStateFlow) {
    intents.send(it)//can use without channel: ```mutableStateFlow.value = reduce(mutableStateFlow.value, it)```
  }

}
