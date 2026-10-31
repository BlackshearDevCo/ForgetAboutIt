package app.forgetaboutit

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

data class Perms(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** The two that reminders can't work without. */
    val essentialOk: Boolean get() = notifications && exactAlarms
}

fun readPerms(ctx: Context): Perms {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    val notifications =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED && nm.areNotificationsEnabled()
    val exact = ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    val full = if (Build.VERSION.SDK_INT >= 34) nm.canUseFullScreenIntent() else true
    val battery = ctx.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(ctx.packageName)
    return Perms(notifications, exact, full, battery)
}

/** Permission status that refreshes whenever the app returns to the foreground. */
@Composable
fun rememberPerms(): State<Perms> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(readPerms(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = readPerms(ctx) }
    return state
}
