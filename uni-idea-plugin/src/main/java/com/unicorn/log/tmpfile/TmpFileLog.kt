package com.unicorn.log.tmpfile

import com.unicorn.log.lib.Log
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object TmpFileLog {
  val timeStr = SimpleDateFormat("yyyy-MM-dd_HH:mm").format(Date())
  val file: File = createTempFile("unicorn-$timeStr-")
  val writer: OutputStreamWriter = file.writer()

  init {
    val calendar = GregorianCalendar()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    Log.info { "log stream to ${file.absolutePath}" }
    Log.addLogConsumer {
      writer.appendln(it.payload.toString())
      writer.flush()//todo flush with delay and coroutines
    }
  }

  fun start() {

  }

}
