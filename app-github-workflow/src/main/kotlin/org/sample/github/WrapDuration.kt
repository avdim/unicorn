package org.sample.github

inline class WrapDuration(val seconds: Long)

fun Collection<WrapDuration>.sum(): WrapDuration {
  val sumSeconds = map {
    it.seconds
  }.reduce { acc, next -> acc + next }
  return WrapDuration(sumSeconds)
}

operator fun WrapDuration.plus(other:WrapDuration):WrapDuration =
  WrapDuration(this.seconds + other.seconds)
