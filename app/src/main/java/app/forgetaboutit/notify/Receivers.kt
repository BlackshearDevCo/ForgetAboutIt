package app.forgetaboutit.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Run suspend work for a broadcast without the system killing the receiver early. */
internal fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Fires at the exact moment something is due. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync { Scheduler.processDue(app) }
    }

    companion object {
        const val ACTION = "app.forgetaboutit.ALARM"
    }
}

/** Re-arms the alarm after a reboot, an app update, or a clock / time zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync { Scheduler.reschedule(app) }
    }
}

/** Handles the Done / Snooze buttons on a notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val reminderId = intent.getLongExtra(EXTRA_REMINDER, -1L)
        val day = intent.getLongExtra(EXTRA_DAY, 0L)
        val slot = intent.getIntExtra(EXTRA_SLOT, -1)
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 10)
        val action = intent.action
        if (reminderId < 0) return
        runAsync {
            when (action) {
                DONE -> Actions.done(app, reminderId, day, slot)
                SNOOZE -> Actions.snooze(app, reminderId, day, slot, minutes)
                SKIP -> Actions.skip(app, reminderId, day, slot)
                else -> Unit
            }
        }
    }

    companion object {
        const val DONE = "app.forgetaboutit.DONE"
        const val SNOOZE = "app.forgetaboutit.SNOOZE"
        const val SKIP = "app.forgetaboutit.SKIP"
        const val EXTRA_REMINDER = "reminderId"
        const val EXTRA_DAY = "day"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_MINUTES = "minutes"
    }
}
