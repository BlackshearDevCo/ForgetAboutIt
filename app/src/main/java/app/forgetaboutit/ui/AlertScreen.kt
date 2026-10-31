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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.forgetaboutit.data.ANYTIME
import app.forgetaboutit.data.Status
import app.forgetaboutit.domain.OccKey
import app.forgetaboutit.domain.epochDayOf
import app.forgetaboutit.domain.millisAt
import app.forgetaboutit.notify.Notifier

/** The "it's time" screen, opened from a notification. */
@Composable
fun AlertScreen(vm: MainViewModel, reminderId: Long, day: Long, slot: Int, onClose: () -> Unit) {
    val reminders by vm.reminders.collectAsState()
    val occs by vm.occurrences.collectAsState()
    val now by vm.now.collectAsState()
    val r = reminders.firstOrNull { it.id == reminderId }

    Column(Modifier.fillMaxSize().background(P.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("REMINDER · ${fmtClock(now)}")
            RoundIconButton(G.Close, "Close", onClose)
        }

        if (r == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("This reminder no longer exists.", color = P.Dim, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        val occ = occs[OccKey(r.id, day, slot)]
        val finished = occ != null && occ.status != Status.PENDING
        val today = epochDayOf(now)
        val due = millisAt(today, if (slot == ANYTIME) r.nudgeFromMinutes else slot)
        val late = (now - due) / 60_000L
        val badge = when {
            occ?.status == Status.DONE -> "Done"
            occ?.status == Status.SKIPPED -> "Skipped for today"
            slot == ANYTIME -> "Still to do today"
            late < 2 -> "Due now · ${fmtTime(slot)}"
            else -> "Overdue ${fmtDuration(late)} · was ${fmtTime(slot)}"
        }
        val nudges = maxOf(1, occ?.nudgeCount ?: 1)

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(200.dp).border(1.5.dp, P.BarLine, CircleShape).padding(28.dp)
                    .border(1.5.dp, P.Line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                IconChip(r.icon, bg = P.Accent, tint = P.OnAccent, size = 96.dp)
            }
            Box(
                Modifier.padding(top = 20.dp).clip(RoundedCornerShape(20.dp)).background(P.Card)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(badge, color = P.Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                r.title, fontSize = 36.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (!finished) {
                Text(
                    "This is nudge $nudges. ${Notifier.nudgeSentence(r.nudge)}",
                    color = P.Dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (finished) {
                PrimaryButton("Back to Today", onClick = onClose)
            } else {
                PrimaryButton(
                    "Done",
                    onClick = {
                        vm.markDone(r.id, day, slot)
                        onClose()
                    },
                    glyph = G.Check,
                    modifier = Modifier.height(64.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in listOf(10, 30, 60)) {
                        Box(
                            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(P.Card)
                                .clickable {
                                    vm.snooze(r.id, day, slot, m)
                                    onClose()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (m == 60) "1 hour" else "$m min",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clickable {
                        vm.skip(r.id, day, slot)
                        onClose()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Skip today", color = P.Dim, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
