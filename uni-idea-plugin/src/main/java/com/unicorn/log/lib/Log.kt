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
    _TODO,
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

  fun breakPoint(message:String) {

  }

  inline fun debug(lambda: () -> Payload) {
    DEBUG_LEVEL_ENABLE {
      handleLog(
        LogLevel.DEBUG,
        lambda()
      )
    }
  }

  inline fun debug(payload: Payload) {
    debug {
      payload
    }
  }

  inline fun todo(payload: Payload) {
    todo {
      payload
    }
  }

  inline fun todo(lambda: () -> Payload) {
    TODO_LEVEL_ENABLE {
      handleLog(
        LogLevel._TODO,
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

  inline fun warning(payload: Payload) {
    warning { payload }
  }

  inline fun error(lambda: () -> Payload) {
    ERROR_LEVEL_ENABLE {
      handleLog(
        LogLevel.ERROR,
        lambda()
      )
    }
  }

  inline fun assertTrue(value:Boolean) {
    if (!value) {
      fatalError { "assertTrue" }
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
    val stackTrace =
      if (false) {
        try {
          throw Exception()
        } catch (t: Throwable) {
          t.stackTrace.drop(1)
        }
      } else {
        listOf()
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
