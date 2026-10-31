package app.forgetaboutit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stroke icons on a 24x24 grid, the same paths used in the design mockups. */
object G {
    val Check = listOf("M5 12l5 5L20 7")
    val Plus = listOf("M12 5v14M5 12h14")
    val Close = listOf("M6 6l12 12M18 6L6 18")
    val Back = listOf("M15 6l-6 6 6 6")
    val ChevronRight = listOf("M9 6l6 6-6 6")
    val ChevronDown = listOf("M6 9l6 6 6-6")
    val Minus = listOf("M5 12h14")
    val Grid = listOf(
        "M5.5 3.5h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z",
        "M15.5 3.5h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z",
        "M5.5 13.5h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z",
        "M15.5 13.5h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z",
    )
    val List = listOf("M9 6h11M9 12h11M9 18h11", "M4.5 6h.01", "M4.5 12h.01", "M4.5 18h.01")
    val Sliders = listOf(
        "M4 7h9M17 7h3M4 17h3M11 17h9",
        "M13 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        "M7 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
    )
    val Clock = listOf("M12 7v5l3 2", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0")
}

/** Icons a reminder can use. Order = order shown in the picker. */
val ReminderIcons: Map<String, List<String>> = linkedMapOf(
    "box" to listOf("M4 7h16v11a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z", "M9 11h6", "M3 7l2-4h14l2 4"),
    "pill" to listOf("M10.5 20.5a5 5 0 0 1-7-7l6-6a5 5 0 0 1 7 7z", "M8.5 8.5l7 7"),
    "bowl" to listOf("M3 11h18a9 9 0 0 1-18 0z", "M9 7c0-1.5 1-2.5 2-3", "M14 7c0-1.5 1-2.5 2-3"),
    "drop" to listOf("M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"),
    "leaf" to listOf(
        "M12 21V11",
        "M12 11c0-4 3-7 8-7 0 5-3 7-8 7z",
        "M12 14c0-3-2.5-5-7-5 0 4 2.5 5 7 5z",
    ),
    "bill" to listOf("M6 2h12v20l-3-2-3 2-3-2-3 2z", "M9 8h6M9 12h6"),
    "cal" to listOf(
        "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
        "M3 10h18", "M8 3v4", "M16 3v4",
    ),
    "home" to listOf("M3 11l9-8 9 8", "M5 10v10h14V10", "M10 20v-6h4v6"),
    "car" to listOf("M5 16v-5l2-5h10l2 5v5", "M3 16h18", "M7 19v-3", "M17 19v-3"),
    "bell" to listOf("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0"),
)

fun iconPaths(key: String): List<String> = ReminderIcons[key] ?: ReminderIcons.getValue("bell")

@Composable
fun Glyph(paths: List<String>, tint: Color, size: Dp, modifier: Modifier = Modifier, stroke: Float = 2f) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            for (p in parsed) {
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
