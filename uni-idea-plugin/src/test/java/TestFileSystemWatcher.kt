import org.junit.Test
import java.nio.file.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class TestFileSystemWatcher {
//  @Test
  fun testTmpDir() {
    val path = Paths.get("/tmp")
    Files.createDirectories(path)
    val list: Stream<Path> = Files.list(path)
    list.use {
      it.forEach {
        println(it)
      }
    }

    val ws = path.fileSystem.newWatchService()
    path.register(
      ws,
      StandardWatchEventKinds.OVERFLOW,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
      StandardWatchEventKinds.ENTRY_MODIFY
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
          println("kind: $kind, context: $context")
        }
      } finally {
        key.reset()
      }
    }
  }
}
