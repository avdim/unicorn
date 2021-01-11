package com.unicorn.log.lib

import java.util.concurrent.CopyOnWriteArrayList

private typealias Payload = Any?

object Log {

  class LogEvent(
    val payload: Payload,
    val logLevel: LogLevel,
    val stackTrace: List<StackTraceElement>
  ) {
    override fun toString(): String {
      return "$logLevel: $payload"
    }
  }

  enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL
  }

  val logConsumers: MutableCollection<(LogEvent) -> Unit> = CopyOnWriteArrayList()
  fun addLogConsumer(consumer: (LogEvent) -> Unit) {
    logConsumers.add(consumer)
  }

  inline fun debug(lambda: () -> Payload) {
    DEBUG_LEVEL_ENABLE {
      handleLog(
        LogLevel.DEBUG,
        lambda()
      )
    }
  }

  inline fun todo(lambda: () -> Payload) {
    TODO_LEVEL_ENABLE {
      handleLog(
        LogLevel.DEBUG,
        lambda()
      )
    }
  }

  inline fun info(payload: Payload){
    info { payload }
  }

  inline fun info(lambda: () -> Payload) {
    INFO_LEVEL_ENABLE {
      handleLog(
        LogLevel.INFO,
        lambda()
      )
    }
  }

  inline fun warning(lambda: () -> Payload) {
    WARNING_LEVEL_ENABLE {
      handleLog(
        LogLevel.WARNING,
        lambda()
      )
    }
  }

  inline fun error(lambda: () -> Payload) {
    ERROR_LEVEL_ENABLE {
      handleLog(
        LogLevel.ERROR,
        lambda()
      )
    }
  }

  inline fun error(payload:Payload) {
    error { payload }
  }

  inline fun fatalError(t: Throwable? = null, lambda: () -> Payload): Nothing {
    ERROR_LEVEL_ENABLE {
      handleLog(
        LogLevel.FATAL,
        lambda()
      )
    }
    throw t ?: Exception("fatal")
  }

  fun handleLog(level: LogLevel, logData: Payload) {
    val stackTrace = try {
      throw Exception()
    } catch (t: Throwable) {
      t.stackTrace.drop(1)
    }

    val logDataWithContext = LogEvent(
      payload = logData,
      logLevel = level,
      stackTrace = stackTrace
    )
    logConsumers.forEach {
      it(
        logDataWithContext
      )
    }
  }
}
