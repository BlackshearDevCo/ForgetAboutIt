@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.forgetaboutit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.forgetaboutit.MainViewModel
import app.forgetaboutit.data.Nudge
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.Repeat
import app.forgetaboutit.data.isAnytime
import app.forgetaboutit.data.parseTimes
import app.forgetaboutit.domain.dayOfWeekIndex
import app.forgetaboutit.domain.epochDayOf

private val Danger = Color(0xFFFFA39B)

private val repeatOptions = listOf(
    Repeat.ONCE to "On a date",
    Repeat.DAILY to "Every day",
    Repeat.WEEKDAYS to "Weekdays",
    Repeat.WEEKLY to "Specific days",
    Repeat.EVERY_N_DAYS to "Every few days",
)

private fun nudgeTitle(n: Nudge) = when (n) {
    Nudge.GENTLE -> "Gentle"
    Nudge.PERSISTENT -> "Persistent"
    Nudge.RELENTLESS -> "Relentless"
}

private fun nudgeDescription(n: Nudge) = when (n) {
    Nudge.GENTLE -> "One reminder at the time"
    Nudge.PERSISTENT -> "Every 10 min until it's done"
    Nudge.RELENTLESS -> "Every 5 min, plus a full-screen alert"
}

@Composable
fun EditorScreen(vm: MainViewModel, reminderId: Long?, onClose: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val today = epochDayOf(System.currentTimeMillis())

    var loaded by remember { mutableStateOf(reminderId == null) }
    var existing by remember { mutableStateOf<Reminder?>(null) }
    var title by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("box") }
    var repeat by remember { mutableStateOf(Repeat.DAILY) }
    var weekdays by remember { mutableStateOf(0) }
    var intervalDays by remember { mutableStateOf(2) }
    var onceDay by remember { mutableStateOf(today) }
    var anytime by remember { mutableStateOf(false) }
    val times = remember { mutableStateListOf(9 * 60) }
    var nudgeFrom by remember { mutableStateOf(16 * 60) }
    var nudge by remember { mutableStateOf(Nudge.PERSISTENT) }
    var nudgeOpen by remember { mutableStateOf(false) }
    /** null = closed, -1 = adding a time, -2 = the "start nudging at" time, >= 0 = editing that time. */
    var timeDialog by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            val r = vm.getReminder(reminderId)
            if (r != null) {
                existing = r
                title = r.title
                icon = r.icon
                repeat = r.repeat
                weekdays = r.weekdays
                intervalDays = if (r.repeat == Repeat.EVERY_N_DAYS) r.intervalDays else 2
                onceDay = if (r.repeat == Repeat.ONCE) r.anchorDay else today
                anytime = r.isAnytime
                times.clear()
                times.addAll(parseTimes(r.times).ifEmpty { listOf(9 * 60) })
                nudgeFrom = r.nudgeFromMinutes
                nudge = r.nudge
            }
            loaded = true
        }
    }

    val canSave = title.isNotBlank()

    fun save() {
        if (!canSave) return
        val base = existing
        val anchor = if (repeat == Repeat.ONCE) onceDay
        else base?.takeIf { it.repeat == repeat }?.anchorDay ?: today
        val r = Reminder(
            id = base?.id ?: 0L,
            title = title.trim(),
            icon = icon,
            repeat = repeat,
            intervalDays = if (repeat == Repeat.EVERY_N_DAYS) intervalDays else 1,
            weekdays = if (repeat == Repeat.WEEKLY) {
                if (weekdays == 0) 1 shl dayOfWeekIndex(today) else weekdays
            } else 0,
            anchorDay = anchor,
            times = if (anytime) "" else times.distinct().sorted().joinToString(","),
            nudgeFromMinutes = nudgeFrom,
            nudge = nudge,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
        )
        vm.save(r) { id -> onSaved(id) }
    }

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (reminderId == null) "New reminder" else "Edit reminder",
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
            )
            RoundIconButton(G.Close, "Close", onClose)
        }

        if (!loaded) return@Column

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ---- name
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconChip(icon, bg = P.Accent, tint = P.OnAccent, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text("Remind me to", color = P.Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = P.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = Manrope,
                        ),
                        cursorBrush = SolidColor(P.Accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Clean litter boxes", color = P.Check, fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }

            // ---- icon
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("ICON")
                for (row in ReminderIcons.keys.toList().chunked(5)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (key in row) {
                            val selected = key == icon
                            Box(
                                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(P.Card)
                                    .then(
                                        if (selected) Modifier.border(2.dp, P.Accent, RoundedCornerShape(14.dp))
                                        else Modifier,
                                    )
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Glyph(iconPaths(key), if (selected) P.Text else P.Dim, 22.dp)
                            }
                        }
                    }
                }
            }

            // ---- repeats
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("REPEATS")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((value, label) in repeatOptions) {
                        OptionChip(
                            label,
                            selected = repeat == value,
                            onClick = {
                                repeat = value
                                // Start with today's weekday lit so the picker is never blank.
                                if (value == Repeat.WEEKLY && weekdays == 0) {
                                    weekdays = 1 shl dayOfWeekIndex(today)
                                }
                            },
                        )
                    }
                }
                when (repeat) {
                    Repeat.WEEKLY -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (i in 0 until 7) {
                            val on = ((weekdays shr i) and 1) == 1
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(if (on) P.Accent else P.Card)
                                    .clickable {
                                        // At least one day must stay selected.
                                        val next = weekdays xor (1 shl i)
                                        if (next != 0) weekdays = next
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    DAY_INITIAL[i], fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (on) P.OnAccent else P.Text,
                                )
                            }
                        }
                    }
                    Repeat.EVERY_N_DAYS -> Stepper(
                        label = "Every $intervalDays days",
                        onMinus = { if (intervalDays > 2) intervalDays-- },
                        onPlus = { if (intervalDays < 60) intervalDays++ },
                    )
                    Repeat.ONCE -> Row(
                        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(P.Card).clickable { dateDialog = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Glyph(iconPaths("cal"), P.Dim, 22.dp)
                        Text(
                            if (onceDay == today) "Today · ${fmtDate(onceDay)}" else fmtDate(onceDay),
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Change", color = P.Accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    else -> Unit
                }
            }

            // ---- when
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("WHEN")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(P.Card).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for ((isAnytimeOption, label) in listOf(false to "At a set time", true to "Anytime today")) {
                        val selected = anytime == isAnytimeOption
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) P.Chip else Color.Transparent)
                                .clickable { anytime = isAnytimeOption },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (selected) P.Text else P.Dim,
                            )
                        }
                    }
                }
                if (anytime) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "If it's not done, start nudging me at",
                            color = P.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp, modifier = Modifier.weight(1f),
                        )
                        TimeChip(fmtTime(nudgeFrom), onClick = { timeDialog = -2 })
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        times.forEachIndexed { index, minutes ->
                            TimeChip(
                                fmtTime(minutes),
                                onClick = { timeDialog = index },
                                onRemove = removeAction(times.size > 1) { times.removeAt(index) },
                            )
                        }
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, P.Check, RoundedCornerShape(14.dp))
                                .clickable { timeDialog = -1 },
                            contentAlignment = Alignment.Center,
                        ) {
                            Glyph(G.Plus, P.Text, 18.dp, stroke = 2.4f)
                        }
                    }
                }
            }

            // ---- nudge style
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.Card)) {
                Row(
                    Modifier.fillMaxWidth().clickable { nudgeOpen = !nudgeOpen }.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Nudge style", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${nudgeTitle(nudge)} · ${nudgeDescription(nudge)}",
                            color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Glyph(if (nudgeOpen) listOf("M6 15l6-6 6 6") else G.ChevronRight, P.Dim, 18.dp, stroke = 2.4f)
                }
                if (nudgeOpen) {
                    for (option in Nudge.values()) {
                        val selected = option == nudge
                        Row(
                            Modifier.fillMaxWidth().clickable { nudge = option }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                Modifier.size(22.dp).border(if (selected) 6.dp else 2.dp, if (selected) P.Accent else P.Check, CircleShape),
                            )
                            Column {
                                Text(nudgeTitle(option), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(nudgeDescription(option), color = P.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (existing != null) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clickable { confirmDelete = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Delete reminder", color = Danger, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp).navigationBarsPadding()) {
            PrimaryButton(
                if (existing == null) "Save reminder" else "Save changes",
                onClick = { save() },
                modifier = Modifier.alpha(if (canSave) 1f else 0.4f),
            )
        }
    }

    val dialog = timeDialog
    if (dialog != null) {
        val initial = when {
            dialog == -2 -> nudgeFrom
            dialog == -1 -> 12 * 60
            dialog < times.size -> times[dialog]
            else -> 9 * 60
        }
        TimeDialog(
            initial = initial,
            onDismiss = { timeDialog = null },
            onConfirm = { minutes ->
                when {
                    dialog == -2 -> nudgeFrom = minutes
                    dialog == -1 -> if (minutes !in times) times.add(minutes)
                    dialog < times.size -> times[dialog] = minutes
                }
                timeDialog = null
            },
        )
    }

    if (dateDialog) {
        val dayMillis = 86_400_000L
        val pickerState = rememberDatePickerState(
            // The picker works in UTC midnights, which map 1:1 onto epoch days.
            initialSelectedDateMillis = onceDay * dayMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis / dayMillis >= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onceDay = maxOf(today, it / dayMillis) }
                    dateDialog = false
                }) { Text("OK", color = P.Accent, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { dateDialog = false }) { Text("Cancel", color = P.Dim) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = P.Card,
            title = { Text("Delete this reminder?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Its history goes too.", color = P.Dim) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    existing?.let { vm.delete(it.id) { onDeleted() } }
                }) { Text("Delete", color = Danger, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = P.Dim) }
            },
        )
    }
}

/** A remove callback when removal is allowed, otherwise null (hides the x). */
private fun removeAction(allowed: Boolean, remove: () -> Unit): (() -> Unit)? =
    if (allowed) remove else null

@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(P.Card).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(G.Minus, "Less", onMinus)
            RoundIconButton(G.Plus, "More", onPlus)
        }
    }
}

@Composable
private fun TimeChip(text: String, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(P.Card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.fillMaxHeight().clickable(onClick = onClick)
                .padding(start = 18.dp, end = if (onRemove == null) 18.dp else 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (onRemove != null) {
            Box(
                Modifier.size(width = 40.dp, height = 48.dp).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(G.Close, P.Dim, 14.dp, stroke = 2.6f)
            }
        }
    }
}

@Composable
private fun TimeDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = P.Card,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("OK", color = P.Accent, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = P.Dim) } },
        text = { TimePicker(state = state) },
    )
}
