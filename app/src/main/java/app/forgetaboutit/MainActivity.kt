package app.forgetaboutit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.forgetaboutit.notify.Notifier
import app.forgetaboutit.ui.AllScreen
import app.forgetaboutit.ui.AlertScreen
import app.forgetaboutit.ui.AppTheme
import app.forgetaboutit.ui.DetailScreen
import app.forgetaboutit.ui.EditorScreen
import app.forgetaboutit.ui.P
import app.forgetaboutit.ui.SettingsScreen
import app.forgetaboutit.ui.TodayScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handleIntent(intent)
        // Make sure the next alarm is armed whenever the app is opened.
        vm.rescheduleAll()
        setContent {
            AppTheme {
                AppRoot(vm, pendingRoute) { pendingRoute = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra(Notifier.EXTRA_ROUTE) ?: return
        pendingRoute = route
        // A reminder alert should be visible even on the lock screen.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }
}

@Composable
private fun AppRoot(vm: MainViewModel, route: String?, onRouteConsumed: () -> Unit) {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(route) {
        if (route != null) {
            nav.navigate(route) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(nav, startDestination = "today", modifier = Modifier.background(P.Bg)) {
        composable("today") {
            TodayScreen(
                vm,
                onOpen = { nav.navigate("detail/$it") },
                onNew = { nav.navigate("new") },
                onAll = { nav.navigate("all") { launchSingleTop = true } },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("all") {
            AllScreen(
                vm,
                onOpen = { nav.navigate("detail/$it") },
                onToday = { nav.popBackStack("today", false) },
                onNew = { nav.navigate("new") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("new") {
            EditorScreen(
                vm, reminderId = null,
                onClose = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
                onDeleted = { nav.popBackStack() },
            )
        }
        composable("edit/{id}", arguments = idArg) { entry ->
            EditorScreen(
                vm, reminderId = entry.arguments?.getLong("id"),
                onClose = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
                onDeleted = { nav.popBackStack("today", false) },
            )
        }
        composable("detail/{id}", arguments = idArg) { entry ->
            DetailScreen(
                vm, reminderId = entry.arguments?.getLong("id") ?: -1L,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("edit/$it") },
            )
        }
        composable(
            "alert/{rid}/{day}/{slot}",
            arguments = listOf(
                navArgument("rid") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType },
                navArgument("slot") { type = NavType.IntType },
            ),
        ) { entry ->
            AlertScreen(
                vm,
                reminderId = entry.arguments?.getLong("rid") ?: -1L,
                day = entry.arguments?.getLong("day") ?: 0L,
                slot = entry.arguments?.getInt("slot") ?: -1,
                onClose = { nav.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
