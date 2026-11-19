package com.timegamblerapp.timer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DayKeys {
  fun forTime(epochMs: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = timeZone
    return format.format(Date(epochMs))
  }
}
