package app.forgetaboutit

import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Nudge
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.Status
import app.forgetaboutit.domain.OccKey
import app.forgetaboutit.domain.Planner
import app.forgetaboutit.domain.Stats
import app.forgetaboutit.domain.byKey
import app.forgetaboutit.domain.millisAt
import app.forgetaboutit.domain.occursOn
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlannerTest {
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /** Saturday, October 31 2026. */
    private val sat = LocalDate.of(2026, 10, 31).toEpochDay()
    private val nineAm = 9 * 60
    private val minute = 60_000L

    private fun reminder(
        repeat: Repeat = Repeat.DAILY,
        anchor: Long = sat - 30,
        times: String = "540",
        nudge: Nudge = Nudge.PERSISTENT,
        weekdays: Int = 0,
        interval: Int = 1,
    ) = Reminder(
        id = 1, title = "Test", repeat = repeat, anchorDay = anchor, times = times,
        nudge = nudge, weekdays = weekdays, intervalDays = interval,
    )

    // ---------------------------------------------------------------- recurrence

    @Test
    fun dailyStartsOnItsAnchorDay() {
        val r = reminder(anchor = sat)
        assertFalse(r.occursOn(sat - 1))
        assertTrue(r.occursOn(sat))
        assertTrue(r.occursOn(sat + 100))
    }

    @Test
    fun weekdaysSkipTheWeekend() {
        val r = reminder(Repeat.WEEKDAYS)
        assertFalse(r.occursOn(sat)) // Saturday
        assertFalse(r.occursOn(sat + 1)) // Sunday
        assertTrue(r.occursOn(sat + 2)) // Monday
    }

    @Test
    fun weeklyFollowsTheChosenDays() {
        val r = reminder(Repeat.WEEKLY, weekdays = 1 shl 5) // Saturdays
        assertTrue(r.occursOn(sat))
        assertFalse(r.occursOn(sat + 1))
        assertTrue(r.occursOn(sat + 7))
    }

    @Test
    fun everyNDaysCountsFromTheAnchor() {
        val r = reminder(Repeat.EVERY_N_DAYS, anchor = sat, interval = 3)
        assertTrue(r.occursOn(sat))
        assertFalse(r.occursOn(sat + 1))
        assertTrue(r.occursOn(sat + 3))
        assertTrue(r.occursOn(sat + 6))
    }

    @Test
    fun onceHappensOnlyOnItsDay() {
        val r = reminder(Repeat.ONCE, anchor = sat)
        assertFalse(r.occursOn(sat - 1))
        assertTrue(r.occursOn(sat))
        assertFalse(r.occursOn(sat + 1))
    }

    // ---------------------------------------------------------------- alerts

    @Test
    fun firstAlertIsAtTheScheduledTime() {
        val t = Planner.nextAlert(reminder(), sat, nineAm, null)
        assertEquals(millisAt(sat, nineAm), t)
    }

    @Test
    fun anytimeReminderStartsNudgingAtTheConfiguredTime() {
        val r = reminder(times = "").copy(nudgeFromMinutes = 16 * 60)
        assertEquals(millisAt(sat, 16 * 60), Planner.nextAlert(r, sat, ANYTIME, null))
    }

    @Test
    fun persistentNudgesEveryTenMinutes() {
        val due = millisAt(sat, nineAm)
        val o = Occurrence(1, sat, nineAm, lastNotifiedAt = due)
        assertEquals(due + 10 * minute, Planner.nextAlert(reminder(), sat, nineAm, o))
    }

    @Test
    fun relentlessNudgesEveryFiveMinutes() {
        val due = millisAt(sat, nineAm)
        val o = Occurrence(1, sat, nineAm, lastNotifiedAt = due)
        assertEquals(due + 5 * minute, Planner.nextAlert(reminder(nudge = Nudge.RELENTLESS), sat, nineAm, o))
    }

    @Test
    fun gentleAlertsOnlyOnce() {
        val o = Occurrence(1, sat, nineAm, lastNotifiedAt = millisAt(sat, nineAm))
        assertNull(Planner.nextAlert(reminder(nudge = Nudge.GENTLE), sat, nineAm, o))
    }

    @Test
    fun doneAndSkippedNeverAlert() {
        val done = Occurrence(1, sat, nineAm, status = Status.DONE)
        val skipped = Occurrence(1, sat, nineAm, status = Status.SKIPPED)
        assertNull(Planner.nextAlert(reminder(), sat, nineAm, done))
        assertNull(Planner.nextAlert(reminder(), sat, nineAm, skipped))
    }

    @Test
    fun nudgingStopsAtMidnight() {
        val r = reminder(times = "1439") // 11:59 PM
        val o = Occurrence(1, sat, 1439, lastNotifiedAt = millisAt(sat, 1439))
        assertNull(Planner.nextAlert(r, sat, 1439, o))
    }

    @Test
    fun snoozeDelaysTheNextAlert() {
        val due = millisAt(sat, nineAm)
        val o = Occurrence(1, sat, nineAm, lastNotifiedAt = due, snoozedUntil = due + 30 * minute)
        assertEquals(due + 30 * minute, Planner.nextAlert(reminder(), sat, nineAm, o))
    }

    // ---------------------------------------------------------------- next wake-up

    @Test
    fun nextTriggerPicksTheEarliestPendingAlert() {
        val early = reminder(times = "480").copy(id = 1) // 8:00
        val late = reminder(times = "1260").copy(id = 2) // 9:00 PM
        val now = millisAt(sat, 7 * 60)
        assertEquals(millisAt(sat, 480), Planner.nextTrigger(listOf(late, early), emptyMap(), now))
    }

    @Test
    fun nextTriggerMovesToTomorrowOnceTodayIsDone() {
        val r = reminder()
        val done = Occurrence(1, sat, nineAm, status = Status.DONE)
        val now = millisAt(sat, 10 * 60)
        val t = Planner.nextTrigger(listOf(r), listOf(done).byKey(), now)
        assertEquals(millisAt(sat + 1, nineAm), t)
    }

    @Test
    fun nextTriggerIsNullWithNothingScheduled() {
        assertNull(Planner.nextTrigger(emptyList(), emptyMap(), millisAt(sat, 0)))
    }

    @Test
    fun overdueOneTimeReminderIsStillPending() {
        val r = reminder(Repeat.ONCE, anchor = sat - 2)
        val items = Planner.itemsFor(sat, listOf(r), emptyMap())
        assertEquals(1, items.size)
        assertEquals(sat - 2, items[0].occDay)
    }

    // ---------------------------------------------------------------- stats

    @Test
    fun streakCountsConsecutiveDoneDaysAndIgnoresPendingToday() {
        val r = reminder(anchor = sat - 10)
        val occ = listOf(
            Occurrence(1, sat - 1, nineAm, status = Status.DONE),
            Occurrence(1, sat - 2, nineAm, status = Status.DONE),
        ).byKey()
        assertEquals(2, Stats.streak(r, sat, occ))
    }

    @Test
    fun streakBreaksOnAMissedDay() {
        val r = reminder(anchor = sat - 10)
        val occ = mapOf(
            OccKey(1, sat, nineAm) to Occurrence(1, sat, nineAm, status = Status.DONE),
            OccKey(1, sat - 2, nineAm) to Occurrence(1, sat - 2, nineAm, status = Status.DONE),
        )
        assertEquals(1, Stats.streak(r, sat, occ))
    }
}
