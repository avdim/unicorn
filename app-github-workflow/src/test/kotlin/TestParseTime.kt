import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class TestParseTime {

  @OptIn(ExperimentalTime::class)
  @Test
  fun testParseTime() {
    //https://github.com/tutu-ru-mobile/ios-core/runs/2352544389
    // https://github.com/Kotlin/kotlinx-datetime
    val startedAt = "2021-04-15T12:03:27Z".toInstant()
    val completedAt = "2021-04-15T12:25:41Z".toInstant()
    val duration = completedAt - startedAt
    println("duration.inSeconds: ${duration.inSeconds}")
    println("duration.inMinutes: ${duration.inMinutes}")
    println(duration.toString(DurationUnit.SECONDS))
    // duration 22m 14s
    Clock.System.now()
    TimeZone.of("Europe/Moscow")
    "2010-06-01T22:19:44.475Z".toInstant()
  }
}
