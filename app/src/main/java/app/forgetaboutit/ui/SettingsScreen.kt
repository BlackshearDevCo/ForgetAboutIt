package app.forgetaboutit.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forgetaboutit.MainViewModel
import app.forgetaboutit.notify.Notifier
import app.forgetaboutit.rememberPerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun launchSettings(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // Some phones don't offer every settings page; fall back to this app's details.
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun openNotificationSettings(ctx: Context) = launchSettings(
    ctx,
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
)

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val perms by rememberPerms()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) openNotificationSettings(ctx)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                message = try {
                    val text = vm.exportJson()
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    }
                    "Backup saved."
                } catch (e: Exception) {
                    "Couldn't save the backup."
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                message = try {
                    val text = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: ""
                    }
                    val count = vm.importJson(text)
                    "Restored $count reminders."
                } catch (e: Exception) {
                    "That file isn't a valid backup."
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIconButton(G.Back, "Back", onBack)
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("MAKE SURE REMINDERS REACH YOU", Modifier.padding(horizontal = 4.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(horizontal = 16.dp)) {
                PermRow(
                    "Notifications", "Needed to show reminders at all.",
                    ok = perms.notifications, actionLabel = "Turn on",
                ) {
                    val runtimeGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.POST_NOTIFICATIONS,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (runtimeGranted) openNotificationSettings(ctx)
                    else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                HorizontalDivider(color = P.Chip)
                PermRow(
                    "Exact alarms", "Lets reminders fire on the minute.",
                    ok = perms.exactAlarms, actionLabel = "Allow",
                ) {
                    launchSettings(
                        ctx,
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")),
                    )
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    HorizontalDivider(color = P.Chip)
                    PermRow(
                        "Full-screen alerts", "Lets \"Relentless\" reminders take over the screen.",
                        ok = perms.fullScreen, actionLabel = "Allow",
                    ) {
                        launchSettings(
                            ctx,
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")),
                        )
                    }
                }
                HorizontalDivider(color = P.Chip)
                PermRow(
                    "Battery", "Set to Unrestricted so Android never delays a reminder.",
                    ok = perms.batteryUnrestricted, actionLabel = "Review",
                ) {
                    launchSettings(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }

            SettingsButton("Send a test notification") { Notifier.showTest(ctx) }

            SectionLabel("BACKUP", Modifier.padding(start = 4.dp, top = 12.dp))
            Text(
                "Reminders live only on this phone. Save a backup file before reinstalling or switching phones.",
                color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            SettingsButton("Save backup") { exportLauncher.launch("forgetaboutit-backup.json") }
            SettingsButton("Restore from backup") {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
            if (message != null) {
                Text(
                    message ?: "", color = P.Accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PermRow(title: String, detail: String, ok: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
        }
        if (ok) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Glyph(G.Check, P.Accent, 16.dp, stroke = 3f)
                Text("On", color = P.Accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(P.Accent)
                    .clickable(onClick = onAction).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, color = P.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SettingsButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(P.Card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
