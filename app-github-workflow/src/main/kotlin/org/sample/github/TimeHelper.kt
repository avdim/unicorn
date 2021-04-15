package org.sample.github

import com.sample.WorkflowRunJob
import com.sample.WorkflowRunJobs
import kotlinx.datetime.toInstant
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

inline class WrapDuration(val seconds: Long)

fun WorkflowRunJobs.calcDuration(): WrapDuration =
  jobs.map {
    it.calcDuration()
  }.sum()

fun Collection<WrapDuration>.sum(): WrapDuration {
  val sumSeconds = map {
    it.seconds
  }.reduce { acc, next -> acc + next }
  return WrapDuration(sumSeconds)
}
