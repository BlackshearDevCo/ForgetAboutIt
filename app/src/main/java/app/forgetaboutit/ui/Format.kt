package app.forgetaboutit.ui

import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.isAnytime
import app.forgetaboutit.data.parseTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val DAY_INITIAL = listOf("M", "T", "W", "T", "F", "S", "S")

/** 9:05 PM from minutes after midnight. */
fun fmtTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}

/** Time of day for an epoch-millis moment, in the phone's time zone. */
fun fmtClock(millis: Long): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return fmtTime(t.hour * 60 + t.minute)
}

fun fmtDuration(minutes: Long): String =
    if (minutes < 60) "$minutes min" else "${minutes / 60} hr"

/** "Sat, Oct 31" */
fun fmtDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

/** "SAT · OCT 31" */
fun fmtHeaderDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEE '·' MMM d", Locale.getDefault()))
        .uppercase(Locale.getDefault())

fun repeatLabel(r: Reminder): String = when (r.repeat) {
    Repeat.ONCE -> "Once"
    Repeat.DAILY -> "Daily"
    Repeat.WEEKDAYS -> "Weekdays"
    Repeat.WEEKLY -> {
        val names = DAY_SHORT.filterIndexed { i, _ -> ((r.weekdays shr i) and 1) == 1 }
        if (names.isEmpty()) "Weekly" else names.joinToString(", ")
    }
    Repeat.EVERY_N_DAYS -> if (r.intervalDays <= 1) "Daily" else "Every ${r.intervalDays} days"
}

fun timesLabel(r: Reminder): String =
    if (r.isAnytime) "Anytime" else parseTimes(r.times).joinToString(", ") { fmtTime(it) }

/** "Anytime · nudges from 4:00 PM" / "7:30 AM" / "7:00 PM · Sat" */
fun whenLabel(r: Reminder): String {
    val repeat = if (r.repeat == Repeat.DAILY) null else repeatLabel(r)
    val timing = if (r.isAnytime) "Anytime · nudges from ${fmtTime(r.nudgeFromMinutes)}" else timesLabel(r)
    return if (repeat == null) timing else "$repeat · $timing"
}
