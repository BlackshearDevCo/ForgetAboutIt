package app.forgetaboutit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forgetaboutit.MainViewModel
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.Status
import app.forgetaboutit.data.isAnytime
import app.forgetaboutit.data.slots
import app.forgetaboutit.domain.Cell
import app.forgetaboutit.domain.OccKey
import app.forgetaboutit.domain.Stats
import app.forgetaboutit.domain.epochDayOf
import app.forgetaboutit.domain.nextOccurrenceDay
import app.forgetaboutit.domain.occursOn
import java.time.LocalDate

@Composable
fun DetailScreen(vm: MainViewModel, reminderId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val reminders by vm.reminders.collectAsState()
    val occ by vm.occurrences.collectAsState()
    val now by vm.now.collectAsState()
    val r = reminders.firstOrNull { it.id == reminderId }
    val today = epochDayOf(now)

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(G.Back, "Back", onBack)
            if (r != null) {
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(P.Card)
                        .clickable { onEdit(r.id) }.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }

        if (r == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("This reminder no longer exists.", color = P.Dim, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        val streak = Stats.streak(r, today, occ)
        val month = Stats.month(r, today, occ)
        val recent = Stats.recent(r, occ, 3)
        val pendingToday = if (r.occursOn(today) || (r.repeat == Repeat.ONCE && r.anchorDay <= today)) {
            r.slots.filter { (occ[OccKey(r.id, if (r.repeat == Repeat.ONCE) r.anchorDay else today, it)]?.status ?: Status.PENDING) == Status.PENDING }
        } else emptyList()
        val unit = if (r.repeat == Repeat.DAILY || r.repeat == Repeat.WEEKDAYS) "days" else "times"

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconChip(r.icon, bg = P.Accent, tint = P.OnAccent, size = 60.dp)
                Column {
                    Text(r.title, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
                    Text(
                        "${repeatLabel(r)} · ${timesLabel(r)}" +
                            if (r.isAnytime) " · nudges from ${fmtTime(r.nudgeFromMinutes)}" else "",
                        color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), buildAnnotatedString { append("$streak $unit") }, "Current streak")
                StatCard(
                    Modifier.weight(1f),
                    buildAnnotatedString {
                        append("${month.done}")
                        withStyle(SpanStyle(color = P.Dim, fontSize = 16.sp)) { append(" / ${month.expected}") }
                    },
                    "Done this month",
                )
            }

            // five weeks, Sunday-first columns like the mockup
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("LAST 5 WEEKS")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Legend(filled = true, "Done")
                        Legend(filled = false, "Missed")
                    }
                }
                val todayDow = LocalDate.ofEpochDay(today).dayOfWeek.value % 7 // Sunday = 0
                val gridStart = today - todayDow - 28
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in listOf("S", "M", "T", "W", "T", "F", "S")) {
                        Text(
                            c, modifier = Modifier.weight(1f), color = P.Dim, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                for (week in 0 until 5) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (dow in 0 until 7) {
                            val day = gridStart + week * 7 + dow
                            val cellState = if (day > today) Cell.NONE else Stats.cell(r, day, today, occ)
                            val shape = RoundedCornerShape(6.dp)
                            val base = Modifier.weight(1f).height(22.dp).clip(shape)
                            when (cellState) {
                                Cell.DONE -> Box(base.background(P.Accent))
                                Cell.MISSED -> Box(base.border(1.5.dp, P.Mute, shape))
                                Cell.TODAY -> Box(base.border(2.dp, P.Accent, shape))
                                Cell.NONE -> Box(base.background(if (day > today || !r.occursOn(day)) P.Chip.copy(alpha = 0.45f) else P.Chip))
                            }
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(horizontal = 16.dp)) {
                    recent.forEachIndexed { index, o ->
                        if (index > 0) HorizontalDivider(color = P.Chip)
                        Row(
                            Modifier.fillMaxWidth().height(52.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when (o.day) {
                                    today -> "Today"
                                    today - 1 -> "Yesterday"
                                    else -> fmtDate(o.day)
                                },
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            )
                            val nudgeText = when (o.nudgeCount) {
                                0 -> "no nudges"
                                1 -> "1 nudge"
                                else -> "${o.nudgeCount} nudges"
                            }
                            Text(
                                "Done ${fmtClock(o.doneAt)} · $nudgeText",
                                color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp).navigationBarsPadding()) {
            if (pendingToday.isNotEmpty()) {
                PrimaryButton(
                    "Mark done for today",
                    glyph = G.Check,
                    onClick = {
                        val day = if (r.repeat == Repeat.ONCE) r.anchorDay else today
                        for (slot in pendingToday) vm.markDone(r.id, day, slot)
                    },
                )
            } else {
                val next = r.nextOccurrenceDay(today + 1)
                val text = when {
                    r.occursOn(today) || (r.repeat == Repeat.ONCE && r.anchorDay <= today) -> "Done for today"
                    next != null -> "Next: ${fmtDate(next)}"
                    else -> "Nothing coming up"
                }
                Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
                    Text(text, color = P.Dim, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: androidx.compose.ui.text.AnnotatedString, label: String) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(P.Card).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        Text(label, color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Legend(filled: Boolean, label: String) {
    val shape = RoundedCornerShape(3.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier.size(9.dp).let {
                if (filled) it.background(P.Accent, shape) else it.border(1.5.dp, P.Mute, shape)
            },
        )
        Text(label, color = P.Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
