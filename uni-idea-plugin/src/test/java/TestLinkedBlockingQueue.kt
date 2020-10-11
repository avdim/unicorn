import org.junit.Test
import java.util.concurrent.LinkedBlockingDeque

class TestLinkedBlockingQueue {

  @Test
  fun basic() {
    val queue = LinkedBlockingDeque<Int>(3)
    queue.add(1)
    queue.add(2)
    queue.add(3)
    println(queue)
//        queue.add(4)
//        println(queue)
  }
}
