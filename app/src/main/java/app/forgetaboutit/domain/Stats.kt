package app.forgetaboutit.domain

import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Status
import app.forgetaboutit.data.slots
import java.time.LocalDate

enum class Cell { DONE, MISSED, TODAY, NONE }

data class MonthStat(val done: Int, val expected: Int)

object Stats {
    fun cell(r: Reminder, day: Long, today: Long, occ: Map<OccKey, Occurrence>): Cell {
        if (!r.occursOn(day) || day > today) return Cell.NONE
        val allDone = r.slots.all { occ[OccKey(r.id, day, it)]?.status == Status.DONE }
        return when {
            allDone -> Cell.DONE
            day == today -> Cell.TODAY
            else -> Cell.MISSED
        }
    }

    /** Consecutive scheduled days completed, ending today (today may still be pending). */
    fun streak(r: Reminder, today: Long, occ: Map<OccKey, Occurrence>): Int {
        var count = 0
        var d = today
        val stop = maxOf(r.anchorDay, today - 400)
        while (d >= stop) {
            when (cell(r, d, today, occ)) {
                Cell.DONE -> count++
                Cell.MISSED -> return count
                Cell.TODAY, Cell.NONE -> Unit // not scheduled, or today not finished yet
            }
            d--
        }
        return count
    }

    fun month(r: Reminder, today: Long, occ: Map<OccKey, Occurrence>): MonthStat {
        val monthStart = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
        var done = 0
        var expected = 0
        for (d in maxOf(monthStart, r.anchorDay)..today) {
            if (!r.occursOn(d)) continue
            for (slot in r.slots) {
                expected++
                if (occ[OccKey(r.id, d, slot)]?.status == Status.DONE) done++
            }
        }
        return MonthStat(done, expected)
    }

    /** Most recent completions, newest first. */
    fun recent(r: Reminder, occ: Map<OccKey, Occurrence>, limit: Int): List<Occurrence> =
        occ.values
            .filter { it.reminderId == r.id && it.status == Status.DONE }
            .sortedByDescending { it.doneAt }
            .take(limit)
}
