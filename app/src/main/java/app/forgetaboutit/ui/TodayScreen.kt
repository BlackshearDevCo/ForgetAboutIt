package app.forgetaboutit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forgetaboutit.MainViewModel
import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Status
import app.forgetaboutit.domain.DayItem
import app.forgetaboutit.domain.Planner
import app.forgetaboutit.domain.epochDayOf
import app.forgetaboutit.rememberPerms

private fun keyOf(i: DayItem) = "${i.reminder.id}_${i.occDay}_${i.slot}"

@Composable
fun TodayScreen(
    vm: MainViewModel,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onAll: () -> Unit,
    onSettings: () -> Unit,
) {
    val reminders by vm.reminders.collectAsState()
    val occ by vm.occurrences.collectAsState()
    val now by vm.now.collectAsState()
    val perms by rememberPerms()
    val today = epochDayOf(now)

    val todayItems = remember(reminders, occ, today) { Planner.itemsFor(today, reminders, occ) }
    val pending = remember(todayItems, now) {
        todayItems.filter { it.status == Status.PENDING }.sortedWith(
            compareBy(
                { if (it.dueMillis(today) <= now) 0 else if (it.slot != ANYTIME) 1 else 2 },
                { it.dueMillis(today) },
            ),
        )
    }
    val finished = remember(todayItems) { todayItems.filter { it.status != Status.PENDING } }
    var showDone by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(today, pending.size, finished.size, onSettings)
            }
            if (!perms.essentialOk) {
                item(span = { GridItemSpan(maxLineSpan) }) { PermissionBanner(onSettings) }
            }
            if (todayItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { DayBar(todayItems, now, today) }
            }
            if (reminders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyState() }
            } else if (pending.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { AllDone() }
            }
            items(pending, key = { keyOf(it) }) { item ->
                Tile(
                    item = item, now = now, today = today,
                    onCheck = { vm.markDone(item) },
                    onClick = { onOpen(item.reminder.id) },
                )
            }
            if (finished.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DoneToggle(finished.size, showDone) { showDone = !showDone }
                }
                if (showDone) {
                    items(finished, key = { "done_" + keyOf(it) }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        DoneRow(item, onUndo = { vm.undo(item) }, onClick = { onOpen(item.reminder.id) })
                    }
                }
            }
        }
        BottomBar(selected = 0, onToday = {}, onNew = onNew, onAll = onAll)
    }
}

@Composable
private fun Header(today: Long, todo: Int, done: Int, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 0.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            SectionLabel(fmtHeaderDate(today))
            Text("Today", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = P.Text)) { append("$todo to do") }
                append(" · $done done")
            },
            color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp, end = 8.dp),
        )
    }
}

@Composable
private fun PermissionBanner(onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card)
            .border(1.5.dp, P.Accent, RoundedCornerShape(20.dp))
            .clickable(onClick = onFix).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Reminders can't reach you yet", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Turn on notifications and exact alarms.",
                color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Text("Fix", color = P.Accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing to remember yet", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Tap + to add the first thing you keep forgetting.",
            color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AllDone() {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("All done for today", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Nothing left to remember.",
            color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** A thin 6 AM - 10 PM strip showing where today's timed reminders fall. */
@Composable
private fun DayBar(items: List<DayItem>, now: Long, today: Long) {
    val startMin = 6 * 60
    val endMin = 22 * 60
    val span = (endMin - startMin).toFloat()
    val nowMin = ((now - app.forgetaboutit.domain.millisAt(today, 0)) / 60_000L).toInt()
    fun frac(min: Int) = ((min - startMin) / span).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(16.dp, 16.dp, 16.dp, 12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val w = size.width
            val cy = size.height / 2
            drawRoundRect(P.Line, Offset(0f, cy - 1.dp.toPx()), androidx.compose.ui.geometry.Size(w, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
            drawRoundRect(P.Mute, Offset(0f, cy - 1.dp.toPx()), androidx.compose.ui.geometry.Size(w * frac(nowMin), 2.dp.toPx()), CornerRadius(1.dp.toPx()))
            for (i in items) {
                if (i.slot == ANYTIME) continue
                val x = w * frac(i.slot)
                val late = i.status == Status.PENDING && i.dueMillis(today) <= now
                when {
                    i.status != Status.PENDING -> drawCircle(P.Mute, 5.dp.toPx(), Offset(x, cy))
                    late -> {
                        drawCircle(P.Accent.copy(alpha = 0.22f), 10.dp.toPx(), Offset(x, cy))
                        drawCircle(P.Accent, 6.dp.toPx(), Offset(x, cy))
                    }
                    else -> {
                        drawCircle(P.Card, 5.dp.toPx(), Offset(x, cy))
                        drawCircle(P.Dim, 5.dp.toPx(), Offset(x, cy), style = Stroke(2.dp.toPx()))
                    }
                }
            }
            val nx = w * frac(nowMin)
            drawLine(P.Text, Offset(nx, 0f), Offset(nx, size.height), strokeWidth = 2.dp.toPx())
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp).height(16.dp)) {
            val labelWidth = 44.dp
            listOf(0f to "6 AM", 0.375f to "Noon", 0.75f to "6 PM", 1f to "10 PM").forEach { (f, label) ->
                val x = (maxWidth * f - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth)
                Text(
                    label, color = P.Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(x = x).width(labelWidth),
                )
            }
        }
    }
}

@Composable
private fun Tile(item: DayItem, now: Long, today: Long, onCheck: () -> Unit, onClick: () -> Unit) {
    val r = item.reminder
    val due = item.dueMillis(today) <= now
    val shape = RoundedCornerShape(20.dp)
    val sub = when {
        item.slot != ANYTIME && due -> "Overdue · ${fmtTime(item.slot)}"
        item.slot != ANYTIME -> "${fmtTime(item.slot)} · ${repeatLabel(r)}"
        due -> "Still to do · ${repeatLabel(r)}"
        else -> "Anytime · ${repeatLabel(r)}"
    }
    Column(
        Modifier
            .height(128.dp)
            .clip(shape)
            .background(P.Card)
            .then(if (due) Modifier.border(2.dp, P.Accent, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            IconChip(
                r.icon,
                bg = if (due) P.Accent else P.Chip,
                tint = if (due) P.OnAccent else P.Text,
            )
            CheckCircle(
                done = false,
                ring = if (due) P.Accent else P.Check,
                label = "Mark ${r.title} done",
                onClick = onCheck,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
            )
        }
        Column {
            Text(r.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp, maxLines = 2)
            Text(
                sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                color = if (due) P.Accent else P.Dim,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DoneToggle(count: Int, open: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(shape).border(1.dp, P.Line, shape)
            .clickable(onClick = onToggle).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glyph(G.Check, P.Dim, 16.dp, stroke = 3f)
            Text("$count done", color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Glyph(if (open) listOf("M6 15l6-6 6 6") else G.ChevronDown, P.Dim, 16.dp, stroke = 2.4f)
    }
}

@Composable
private fun DoneRow(item: DayItem, onUndo: () -> Unit, onClick: () -> Unit) {
    val skipped = item.status == Status.SKIPPED
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            done = true, ring = P.Check, label = "Undo ${item.reminder.title}",
            onClick = onUndo, diameter = 22.dp,
        )
        Column(Modifier.padding(start = 6.dp)) {
            Text(
                item.reminder.title, color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                if (skipped) "Skipped" else "Done " + fmtClock(item.occ?.doneAt ?: 0L),
                color = P.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
