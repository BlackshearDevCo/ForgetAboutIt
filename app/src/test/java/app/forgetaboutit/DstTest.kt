package app.forgetaboutit

import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.Status
import app.forgetaboutit.domain.Planner
import app.forgetaboutit.domain.byKey
import app.forgetaboutit.domain.epochDayOf
import app.forgetaboutit.domain.millisAt
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Daylight saving changes must never shift what the clock says. Uses the real 2026 Denver dates. */
class DstTest {
    private val denver = ZoneId.of("America/Denver")
    private lateinit var original: TimeZone

    @Before
    fun setUp() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(denver))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(original)
    }

    private fun wall(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, denver).toInstant().toEpochMilli()

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun fallBackDayKeepsClockTimes() {
        // Nov 1 2026 is 25 hours long. 9:00 AM must still be 9:00 AM on the wall clock.
        assertEquals(wall(2026, 11, 1, 9), millisAt(day(2026, 11, 1), 9 * 60))
        assertEquals(wall(2026, 11, 1, 21, 30), millisAt(day(2026, 11, 1), 21 * 60 + 30))
    }

    @Test
    fun springForwardDayKeepsClockTimes() {
        // Mar 8 2026 is 23 hours long.
        assertEquals(wall(2026, 3, 8, 9), millisAt(day(2026, 3, 8), 9 * 60))
        assertEquals(wall(2026, 3, 8, 21), millisAt(day(2026, 3, 8), 21 * 60))
    }

    @Test
    fun midnightAfterALongDayIsStillMidnight() {
        assertEquals(wall(2026, 11, 2, 0), millisAt(day(2026, 11, 2), 0))
        assertEquals(wall(2026, 11, 2, 0), millisAt(day(2026, 11, 1) + 1, 0))
    }

    @Test
    fun aTimeThatDoesNotExistMovesForward() {
        // 2:30 AM on Mar 8 2026 never happens; it becomes 3:30 AM.
        assertEquals(wall(2026, 3, 8, 3, 30), millisAt(day(2026, 3, 8), 2 * 60 + 30))
    }

    @Test
    fun epochDayFollowsTheLocalCalendarAcrossTheChange() {
        assertEquals(day(2026, 11, 1), epochDayOf(wall(2026, 11, 1, 0, 30)))
        assertEquals(day(2026, 11, 1), epochDayOf(wall(2026, 11, 1, 23, 30)))
        assertEquals(day(2026, 11, 2), epochDayOf(wall(2026, 11, 2, 0, 30)))
    }

    @Test
    fun alarmSetBeforeTheChangeLandsOnTheRightClockTime() {
        // Saturday night, before the clocks go back: tomorrow's 9:00 AM alarm is computed
        // across the transition and must come out as 9:00 AM Sunday, not 8:00 AM.
        val r = Reminder(id = 1, title = "Pills", repeat = Repeat.DAILY, anchorDay = day(2026, 10, 1), times = "540")
        val doneSaturday = Occurrence(1, day(2026, 10, 31), 540, status = Status.DONE)
        val now = wall(2026, 10, 31, 20)
        assertEquals(wall(2026, 11, 1, 9), Planner.nextTrigger(listOf(r), listOf(doneSaturday).byKey(), now))
    }
}
