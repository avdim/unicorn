package org.sample.github

inline class WrapDuration(val seconds: Long) {
  override fun toString(): String {
    return "${seconds / 60 } m + ${seconds % 60} s"
  }
}

fun Collection<WrapDuration>.sum(): WrapDuration {
  val sumSeconds = map {
    it.seconds
  }.reduce { acc, next -> acc + next }
  return WrapDuration(sumSeconds)
}

operator fun WrapDuration.plus(other:WrapDuration):WrapDuration =
  WrapDuration(this.seconds + other.seconds)
