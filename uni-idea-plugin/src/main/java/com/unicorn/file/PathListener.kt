package com.unicorn.file

import com.unicorn.Uni
import kotlinx.coroutines.*
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey

class PathListenerEvent(val type: Type, val path: Path) {
  sealed class Type {
    object New : Type()
    object Delete : Type()
  }
}

fun Path.addListener(scope: CoroutineScope, listener: (PathListenerEvent) -> Unit): Job {
  val dir = this

  val job = scope.launch(context = Dispatchers.IO) {
    val ws = dir.fileSystem.newWatchService()
    dir.register(
      ws,
      StandardWatchEventKinds.OVERFLOW,//todo
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
//      StandardWatchEventKinds.ENTRY_MODIFY
    )

    val key: WatchKey = try {
      ws.take()
    } catch (t: Throwable) {
      Uni.log.fatalError(t) { "WatchKey take() fail" }
    }
    try {
      while (isActive) {
        val events = key.pollEvents()
        events.forEach {
          val kind = it.kind()
          val context = it.context()
          val p = (context as? Path)?.let {
            dir.resolve(it)
          }
          Uni.log.info { "kind: $kind, context: $context" }
          when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
              if (p != null) {
                withContext(Dispatchers.Main) {//todo Main?
                  listener(PathListenerEvent(PathListenerEvent.Type.New, p))
                }
              }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
              if (p != null) {
                withContext(Dispatchers.Main) {//todo Main?
                  listener(PathListenerEvent(PathListenerEvent.Type.Delete, p))
                }
              }
            }
          }
        }
        yield()
      }
    } finally {
      key.reset()
      Uni.log.info { "path listener finally, dir: $dir" }
    }
  }
  return job
}
