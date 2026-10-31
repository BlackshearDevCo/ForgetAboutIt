package app.forgetaboutit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Repeat { ONCE, DAILY, WEEKDAYS, WEEKLY, EVERY_N_DAYS }

/** GENTLE = one alert. PERSISTENT = again every 10 min. RELENTLESS = every 5 min + full-screen alert. */
enum class Nudge { GENTLE, PERSISTENT, RELENTLESS }

enum class Status { PENDING, DONE, SKIPPED }

/** Slot value meaning "anytime today" (no fixed time). */
const val ANYTIME = -1

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Key into the icon set in ui/Glyphs.kt. */
    val icon: String = "bell",
    val repeat: Repeat = Repeat.DAILY,
    /** Used by EVERY_N_DAYS. */
    val intervalDays: Int = 1,
    /** Used by WEEKLY: bit 0 = Monday ... bit 6 = Sunday. */
    val weekdays: Int = 0,
    /** Epoch day. ONCE: the date. Others: first day the reminder applies. */
    val anchorDay: Long,
    /** Comma separated minutes since midnight. Empty = anytime today. */
    val times: String = "",
    /** For anytime reminders: when nudging starts (minutes since midnight). */
    val nudgeFromMinutes: Int = 16 * 60,
    val nudge: Nudge = Nudge.PERSISTENT,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

/** One concrete "thing to do": a reminder, on a day, at a slot. Created lazily. */
@Entity(tableName = "occurrences", primaryKeys = ["reminderId", "day", "slot"])
data class Occurrence(
    val reminderId: Long,
    val day: Long,
    val slot: Int,
    val status: Status = Status.PENDING,
    val doneAt: Long = 0,
    val nudgeCount: Int = 0,
    val lastNotifiedAt: Long = 0,
    val snoozedUntil: Long = 0,
)

fun parseTimes(csv: String): List<Int> =
    csv.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()

val Reminder.slots: List<Int>
    get() = parseTimes(times).ifEmpty { listOf(ANYTIME) }

val Reminder.isAnytime: Boolean
    get() = parseTimes(times).isEmpty()
