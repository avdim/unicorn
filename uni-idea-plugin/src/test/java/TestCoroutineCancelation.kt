import kotlinx.coroutines.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.suspendCoroutine

class TestCoroutineCancelation {

  @Test
  fun testCancellation() {
    runBlocking {
      val job = Job()
      val scope = CoroutineScope(job)
      GlobalScope.launch {
        delay(100)
        job.cancel()
      }
      scope.launch {
        println("- inside scope")
        try {
          suspendCancellableCoroutine<Unit> { continuation ->

          }
          println("- never printed")
        } finally {
          println("- finally")
        }
      }.join()
    }
  }
}

