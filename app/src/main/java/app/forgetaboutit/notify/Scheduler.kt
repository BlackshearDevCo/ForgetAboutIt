package app.forgetaboutit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Status
import app.forgetaboutit.data.dao
import app.forgetaboutit.data.slots
import app.forgetaboutit.domain.OccKey
import app.forgetaboutit.domain.Planner
import app.forgetaboutit.domain.byKey
import app.forgetaboutit.domain.epochDayOf
import app.forgetaboutit.domain.millisAt
import app.forgetaboutit.domain.occursOn
import app.forgetaboutit.data.Repeat

/**
 * Keeps exactly one exact alarm set: the earliest moment anything needs attention.
 * When it fires, [processDue] posts whatever is due and sets the next one.
 */
object Scheduler {
    private const val REQUEST_CODE = 4242

    private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST_CODE,
        Intent(ctx, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    suspend fun reschedule(ctx: Context) {
        val dao = ctx.dao()
        val reminders = dao.reminders()
        val occ = dao.occurrences().byKey()
        val next = Planner.nextTrigger(reminders, occ, System.currentTimeMillis())
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx)
        if (next == null) {
            am.cancel(pi)
            return
        }
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    /** Post everything that is due right now, then arm the next alarm. */
    suspend fun processDue(ctx: Context) {
        val dao = ctx.dao()
        val now = System.currentTimeMillis()
        val today = epochDayOf(now)
        val reminders = dao.reminders()
        val occ = dao.occurrences().byKey()

        for (item in Planner.itemsFor(today, reminders, occ)) {
            val r = item.reminder
            val o = occ[OccKey(r.id, item.occDay, item.slot)]
            val due = Planner.nextAlert(r, today, item.slot, o) ?: continue
            if (due > now + 2_000) continue
            val updated = (o ?: Occurrence(r.id, item.occDay, item.slot)).copy(
                lastNotifiedAt = now,
                nudgeCount = (o?.nudgeCount ?: 0) + 1,
            )
            dao.upsert(updated)
            Notifier.show(ctx, r, updated, today, now)
        }
        reschedule(ctx)
    }

    /**
     * Called after a reminder is created or edited. Avoids an instant burst of alerts for
     * times that already passed earlier today.
     */
    suspend fun onSaved(ctx: Context, r: Reminder) {
        val dao = ctx.dao()
        val now = System.currentTimeMillis()
        val today = epochDayOf(now)
        val applies = if (r.repeat == Repeat.ONCE) r.anchorDay == today else r.occursOn(today)
        if (applies) {
            for (slot in r.slots) {
                if (dao.occurrence(r.id, today, slot) != null) continue
                val base = millisAt(today, if (slot == ANYTIME) r.nudgeFromMinutes else slot)
                if (base >= now) continue
                if (slot == ANYTIME) {
                    // Already past the "start nudging" time: begin the nudge cycle from now.
                    dao.upsert(Occurrence(r.id, today, slot, lastNotifiedAt = now))
                } else if (now - base > 30 * 60_000L) {
                    // Added well after its time today; don't alert for a slot that is long gone.
                    dao.upsert(Occurrence(r.id, today, slot, status = Status.SKIPPED))
                }
            }
        }
        reschedule(ctx)
    }
}
