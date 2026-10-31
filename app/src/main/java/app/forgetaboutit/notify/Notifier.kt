package app.forgetaboutit.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.forgetaboutit.MainActivity
import app.forgetaboutit.R
import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Nudge
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.domain.Planner
import app.forgetaboutit.domain.millisAt
import app.forgetaboutit.ui.fmtDuration
import app.forgetaboutit.ui.fmtTime
import java.util.Objects
import kotlin.math.abs

object Notifier {
    const val CHANNEL = "reminders"
    const val EXTRA_ROUTE = "route"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders and nudges for things you need to do"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    fun notificationId(reminderId: Long, day: Long, slot: Int): Int =
        abs(Objects.hash(reminderId, day, slot))

    fun cancel(ctx: Context, reminderId: Long, day: Long, slot: Int) {
        ctx.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(reminderId, day, slot))
    }

    fun alertRoute(reminderId: Long, day: Long, slot: Int) = "alert/$reminderId/$day/$slot"

    /** Posts (or re-posts) the notification for one occurrence. [timingDay] supplies the clock times. */
    fun show(ctx: Context, r: Reminder, o: Occurrence, timingDay: Long, now: Long) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        ensureChannel(ctx)

        val nid = notificationId(o.reminderId, o.day, o.slot)
        val route = alertRoute(o.reminderId, o.day, o.slot)

        val open = PendingIntent.getActivity(
            ctx, nid,
            Intent(ctx, MainActivity::class.java)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val text = if (o.slot == ANYTIME) {
            if (o.nudgeCount <= 1) "On your list for today" else "Still to do today"
        } else {
            val due = millisAt(timingDay, o.slot)
            val late = (now - due) / 60_000L
            if (late < 2) "Due now · ${fmtTime(o.slot)}"
            else "Overdue ${fmtDuration(late)} · was ${fmtTime(o.slot)}"
        }

        val builder = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(r.title)
            .setContentText(if (o.nudgeCount > 1) "$text · nudge ${o.nudgeCount}" else text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF9DB4FF.toInt())
            .setAutoCancel(false)
            .setContentIntent(open)
            .addAction(0, "Done", action(ctx, ActionReceiver.DONE, o, nid, 1))
            .addAction(0, "Snooze 10 min", action(ctx, ActionReceiver.SNOOZE, o, nid, 2))

        if (r.nudge == Nudge.RELENTLESS) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setFullScreenIntent(open, true)
        }

        ctx.getSystemService(NotificationManager::class.java).notify(nid, builder.build())
    }

    private fun action(ctx: Context, act: String, o: Occurrence, nid: Int, index: Int): PendingIntent {
        val intent = Intent(ctx, ActionReceiver::class.java)
            .setAction(act)
            .putExtra(ActionReceiver.EXTRA_REMINDER, o.reminderId)
            .putExtra(ActionReceiver.EXTRA_DAY, o.day)
            .putExtra(ActionReceiver.EXTRA_SLOT, o.slot)
            .putExtra(ActionReceiver.EXTRA_MINUTES, 10)
        return PendingIntent.getBroadcast(
            ctx, nid * 4 + index, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** A plain notification so you can check delivery works on this phone. */
    fun showTest(ctx: Context) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Test reminder")
            .setContentText("If you can see this, notifications are working.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF9DB4FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(1, n)
    }

    /** Text for the "I'll check back" line, shared by the alert screen. */
    fun nudgeSentence(n: Nudge): String {
        val minutes = Planner.nudgeIntervalMillis(n)?.div(60_000L)
        return if (minutes == null) "I won't nag about this one."
        else "I'll check back every $minutes minutes until it's done."
    }
}
