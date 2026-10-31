package app.forgetaboutit.notify

import android.content.Context
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Status
import app.forgetaboutit.data.dao

/** User actions on one occurrence. Shared by the screens and the notification buttons. */
object Actions {
    private suspend fun change(
        ctx: Context, reminderId: Long, day: Long, slot: Int,
        edit: (Occurrence) -> Occurrence,
    ) {
        val dao = ctx.dao()
        val current = dao.occurrence(reminderId, day, slot) ?: Occurrence(reminderId, day, slot)
        dao.upsert(edit(current))
        Notifier.cancel(ctx, reminderId, day, slot)
        Scheduler.reschedule(ctx)
    }

    suspend fun done(ctx: Context, reminderId: Long, day: Long, slot: Int) =
        change(ctx, reminderId, day, slot) {
            it.copy(status = Status.DONE, doneAt = System.currentTimeMillis())
        }

    /** Takes a completion back. Treated as just-alerted so it doesn't fire again instantly. */
    suspend fun undo(ctx: Context, reminderId: Long, day: Long, slot: Int) =
        change(ctx, reminderId, day, slot) {
            it.copy(status = Status.PENDING, doneAt = 0, lastNotifiedAt = System.currentTimeMillis())
        }

    suspend fun skip(ctx: Context, reminderId: Long, day: Long, slot: Int) =
        change(ctx, reminderId, day, slot) { it.copy(status = Status.SKIPPED) }

    suspend fun snooze(ctx: Context, reminderId: Long, day: Long, slot: Int, minutes: Int) =
        change(ctx, reminderId, day, slot) {
            it.copy(snoozedUntil = System.currentTimeMillis() + minutes * 60_000L)
        }
}
