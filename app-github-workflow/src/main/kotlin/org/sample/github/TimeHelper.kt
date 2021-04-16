package org.sample.github

import com.sample.WorkflowRun
import com.sample.WorkflowRunJob
import com.sample.WorkflowRunJobs
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Month
import kotlin.time.ExperimentalTime

val THREE_HOURS = WrapDuration(3 * 60 * 60)

@OptIn(ExperimentalTime::class)
fun WorkflowRunJob.calcDuration(): WrapDuration {
  val a = startedAt
  val b = completedAt
  if (a != null && b != null) {
    try {
      val secA = a.toInstant().epochSeconds
      val secB = b.toInstant().epochSeconds
      val durationSec = secB - secA
      if (durationSec >= 0) {
        return WrapDuration(durationSec)
      } else {
        return THREE_HOURS
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      return THREE_HOURS
    }
  } else {
    return WrapDuration(0)
  }
}

fun WorkflowRunJobs.calcDuration(): WrapDuration =
  jobs.map {
    it.calcDuration()
  }.sum()

val WorkflowRun.createdTime get() = createdAt.toInstant()
val march29 = LocalDateTime(2021, Month.MARCH, 29, 0, 0).toInstant(TimeZone.UTC)
fun WorkflowRun.isCreatedAfter29March() = createdTime > march29
