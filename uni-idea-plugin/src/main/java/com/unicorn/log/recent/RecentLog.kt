package com.unicorn.log.recent

import com.unicorn.Uni
import com.unicorn.log.lib.Log
import java.util.concurrent.CopyOnWriteArrayList

object RecentLog {

  private val _logs: MutableList<Log.LogEvent> = CopyOnWriteArrayList()
  val logs get():List<Log.LogEvent> = _logs

  init {
    Log.addLogConsumer {
      while (logs.size >= RECENT_LOG_SIZE) {
        if(_logs.isNotEmpty()) {
          _logs.removeAt(0)
        }
      }
      _logs.add(it)
    }
  }

  fun start() {
    Uni.log.info { "RecentLog start()" }
  }

  fun clearLogs() {
    _logs.clear()
  }

}
