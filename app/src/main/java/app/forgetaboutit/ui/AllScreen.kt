package app.forgetaboutit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forgetaboutit.MainViewModel
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat

@Composable
fun AllScreen(
    vm: MainViewModel,
    onOpen: (Long) -> Unit,
    onToday: () -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
) {
    val reminders by vm.reminders.collectAsState()

    val everyDay = reminders.filter { it.repeat == Repeat.DAILY }
    val repeating = reminders.filter {
        it.repeat == Repeat.WEEKDAYS || it.repeat == Repeat.WEEKLY || it.repeat == Repeat.EVERY_N_DAYS
    }
    val oneTime = reminders.filter { it.repeat == Repeat.ONCE }

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 28.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                SectionLabel(if (reminders.size == 1) "1 REMINDER" else "${reminders.size} REMINDERS")
                Text("All reminders", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
            }
            RoundIconButton(G.Sliders, "Settings", onSettings)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (reminders.isEmpty()) {
                Text(
                    "Nothing here yet. Tap + to add your first reminder.",
                    color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                )
            }
            Group("EVERY DAY", everyDay, onOpen)
            Group("REPEATING", repeating, onOpen)
            Group("ONE TIME", oneTime, onOpen)
        }
        BottomBar(selected = 1, onToday = onToday, onNew = onNew, onAll = {})
    }
}

@Composable
private fun Group(label: String, items: List<Reminder>, onOpen: (Long) -> Unit) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label, Modifier.padding(horizontal = 8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(horizontal = 12.dp)) {
            items.forEachIndexed { index, r ->
                if (index > 0) HorizontalDivider(color = P.Chip)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { onOpen(r.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconChip(r.icon, bg = P.Chip, tint = P.Text, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(r.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(whenLabel(r), color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Glyph(G.ChevronRight, P.Check, 16.dp, stroke = 2.4f)
                }
            }
        }
    }
}
