package app.forgetaboutit.domain

import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Nudge
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.Status
import app.forgetaboutit.data.slots
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------------------------------------------------------------- time helpers

fun epochDayOf(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Wall-clock time [minutes] after midnight on [day], in the phone's time zone. */
fun millisAt(day: Long, minutes: Int): Long =
    LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault())
        .plusMinutes(minutes.toLong()).toInstant().toEpochMilli()

/** Monday = 0 ... Sunday = 6. */
fun dayOfWeekIndex(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1

// ---------------------------------------------------------------- recurrence

fun Reminder.occursOn(day: Long): Boolean = when (repeat) {
    Repeat.ONCE -> day == anchorDay
    Repeat.DAILY -> day >= anchorDay
    Repeat.WEEKDAYS -> day >= anchorDay && dayOfWeekIndex(day) in 0..4
    Repeat.WEEKLY -> day >= anchorDay && ((weekdays shr dayOfWeekIndex(day)) and 1) == 1
    Repeat.EVERY_N_DAYS -> day >= anchorDay && (day - anchorDay) % maxOf(1, intervalDays) == 0L
}

/** The next day (today or later, within a year) this reminder occurs, or null. */
fun Reminder.nextOccurrenceDay(from: Long): Long? {
    if (repeat == Repeat.ONCE) return if (anchorDay >= from) anchorDay else null
    for (d in from..from + 400) if (occursOn(d)) return d
    return null
}

// ---------------------------------------------------------------- occurrence bookkeeping

data class OccKey(val reminderId: Long, val day: Long, val slot: Int)

fun List<Occurrence>.byKey(): Map<OccKey, Occurrence> =
    associateBy { OccKey(it.reminderId, it.day, it.slot) }

/** One row on the Today screen. */
data class DayItem(
    val reminder: Reminder,
    /** The day this item belongs to (earlier than today for an overdue one-time reminder). */
    val occDay: Long,
    val slot: Int,
    val occ: Occurrence?,
) {
    val status: Status get() = occ?.status ?: Status.PENDING

    /** When this item first becomes due today. */
    fun dueMillis(today: Long): Long =
        millisAt(today, if (slot == ANYTIME) reminder.nudgeFromMinutes else slot)
}

object Planner {
    private const val MINUTE = 60_000L

    /** Minutes between nudges, or null when the style only alerts once. */
    fun nudgeIntervalMillis(n: Nudge): Long? = when (n) {
        Nudge.GENTLE -> null
        Nudge.PERSISTENT -> 10 * MINUTE
        Nudge.RELENTLESS -> 5 * MINUTE
    }

    /** Everything that belongs on the Today screen (pending, done and skipped). */
    fun itemsFor(today: Long, reminders: List<Reminder>, occ: Map<OccKey, Occurrence>): List<DayItem> {
        val out = ArrayList<DayItem>()
        for (r in reminders) {
            if (r.repeat == Repeat.ONCE) {
                if (r.anchorDay > today) continue
                for (slot in r.slots) {
                    val o = occ[OccKey(r.id, r.anchorDay, slot)]
                    val pending = (o?.status ?: Status.PENDING) == Status.PENDING
                    // A one-time reminder stays on Today until it is dealt with.
                    if (r.anchorDay == today || pending) out.add(DayItem(r, r.anchorDay, slot, o))
                }
            } else if (r.occursOn(today)) {
                for (slot in r.slots) out.add(DayItem(r, today, slot, occ[OccKey(r.id, today, slot)]))
            }
        }
        return out
    }

    /**
     * When should this occurrence next alert, or null if never (done, skipped, gentle and
     * already alerted, or the day has ended)?
     * [timingDay] is the day whose clock times apply.
     */
    fun nextAlert(r: Reminder, timingDay: Long, slot: Int, o: Occurrence?): Long? {
        if (o != null && o.status != Status.PENDING) return null
        val base = millisAt(timingDay, if (slot == ANYTIME) r.nudgeFromMinutes else slot)
        val last = o?.lastNotifiedAt ?: 0L
        var t: Long
        if (last < base) {
            t = base
        } else {
            val interval = nudgeIntervalMillis(r.nudge) ?: return null
            t = last + interval
        }
        val snoozed = o?.snoozedUntil ?: 0L
        if (snoozed > last) t = maxOf(t, snoozed)
        // Nudging stops at midnight; tomorrow is a fresh start.
        if (t >= millisAt(timingDay + 1, 0)) return null
        return t
    }

    /** The single earliest moment the app needs to wake up for. */
    fun nextTrigger(reminders: List<Reminder>, occ: Map<OccKey, Occurrence>, now: Long): Long? {
        val today = epochDayOf(now)
        var best: Long? = null
        for (r in reminders) {
            for (slot in r.slots) {
                var t: Long? = null
                if (r.repeat == Repeat.ONCE) {
                    val timing = maxOf(r.anchorDay, today)
                    t = nextAlert(r, timing, slot, occ[OccKey(r.id, r.anchorDay, slot)])
                } else {
                    var d = today
                    while (d <= today + 400) {
                        if (r.occursOn(d)) {
                            t = nextAlert(r, d, slot, occ[OccKey(r.id, d, slot)])
                            if (t != null) break
                        }
                        d++
                    }
                }
                val b = best
                if (t != null && (b == null || t < b)) best = t
            }
        }
        return best
    }
}
