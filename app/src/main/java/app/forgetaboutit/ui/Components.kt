package app.forgetaboutit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small all-caps label above a group, e.g. "REPEATS". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = P.Dim,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.7.sp,
    )
}

/** Rounded square holding a reminder icon. */
@Composable
fun IconChip(icon: String, bg: Color, tint: Color, size: Dp = 34.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(iconPaths(icon), tint, size * 0.53f)
    }
}

/** Round checkbox with a 44dp touch target. */
@Composable
fun CheckCircle(
    done: Boolean,
    ring: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 26.dp,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Box(Modifier.size(diameter).clip(CircleShape).background(P.Check), contentAlignment = Alignment.Center) {
                Glyph(G.Check, P.Bg, diameter * 0.55f, stroke = 3.4f)
            }
        } else {
            Box(Modifier.size(diameter).border(2.dp, ring, CircleShape))
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: List<String>? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(P.Accent)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            Glyph(glyph, P.OnAccent, 20.dp, stroke = 3f)
            Box(Modifier.size(8.dp))
        }
        Text(text, color = P.OnAccent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Selectable pill used for repeat options, nudge intervals and so on. */
@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) P.Accent else P.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) P.OnAccent else P.Text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

/** Round 44dp icon button, used for back / close / settings. */
@Composable
fun RoundIconButton(glyph: List<String>, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(P.Card)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(glyph, P.Text, 20.dp, stroke = 2.4f)
    }
}

@Composable
private fun NavItem(glyph: List<String>, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(glyph, if (active) P.Accent else P.Dim, 26.dp, stroke = 2.2f)
    }
}

/** Bottom bar: Today, a raised "+" button, All reminders. [selected]: 0 = Today, 1 = All. */
@Composable
fun BottomBar(selected: Int, onToday: () -> Unit, onNew: () -> Unit, onAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(P.Bar)) {
        HorizontalDivider(color = P.BarLine)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(G.Grid, "Today", selected == 0, onToday)
            Box(
                Modifier
                    .offset(y = (-14).dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(P.Accent)
                    .border(4.dp, P.Bg, CircleShape)
                    .semantics { contentDescription = "New reminder" }
                    .clickable(onClick = onNew),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(G.Plus, P.OnAccent, 26.dp, stroke = 2.6f)
            }
            NavItem(G.List, "All reminders", selected == 1, onAll)
        }
    }
}
