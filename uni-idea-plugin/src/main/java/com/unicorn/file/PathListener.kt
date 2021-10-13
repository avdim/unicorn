package com.unicorn.file

import kotlinx.coroutines.*
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.concurrent.thread

class PathListenerEvent(val type: Type, val path: Path) {
  sealed class Type {
    object New : Type()
    object Delete : Type()
  }
}

fun Path.addListener(scope:CoroutineScope, listener: (PathListenerEvent) -> Unit):Job {
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

    while (true) {
      val key: WatchKey = try {
        ws.take()
      } catch (t: Throwable) {
        println("catch")
        continue
      }
      try {
        val events = key.pollEvents()
        events.forEach {
          val kind = it.kind()
          val context = it.context()
          val p = (context as? Path)?.let {
            dir.resolve(it)
          }
          println("kind: $kind, context: $context")
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
      } finally {
        key.reset()
      }
      yield()
    }
  }

  return job
}
