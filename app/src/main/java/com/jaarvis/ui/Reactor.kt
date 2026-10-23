package com.jaarvis.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaarvis.core.ReactorState
import kotlin.math.*

val Cyan = Color(0xFF62E9FF)
val Muted = Color(0xFF91AFC5)
val Panel = Color(0xFF0B1B2D)
val Ink = Color(0xFF030B17)

enum class Module(val subtitle: String, val code: String) {
    INTELLIGENCE("Local command routing", "01"), OPERATIONS("Device actions", "02"),
    RESEARCH("Search handoff", "03"), MEMORY("Private local notes", "04"),
    ENGINEERING("Execution diagnostics", "05"), FINANCE("Budget calculator", "06"),
    SYSTEM("Device & preferences", "07"), AUTOMATIONS("Clock alarms & timers", "08")
}

/** A single lifecycle-gated clock drives bounded draw work. No random state or per-particle jobs. */
@Composable
fun ReactorMap(state: ReactorState, level: Float, motion: Float, animate: Boolean,
               onListen: () -> Unit, onModule: (Module) -> Unit) {
    val rotation: State<Float> = if (animate && motion > 0f) {
        val clock = rememberInfiniteTransition(label = "reactor clock")
        clock.animateFloat(0f, 360f, infiniteRepeatable(tween((30000 / motion.coerceAtLeast(.2f)).toInt(), easing = LinearEasing)), label = "orbit")
    } else remember { mutableFloatStateOf(0f) }
    val stateColor = when (state) {
        ReactorState.ERROR -> Color(0xFFFFB4A5)
        ReactorState.THINKING -> Color(0xFFC5B4FF)
        ReactorState.PLANNING -> Color(0xFF9DC6FF)
        ReactorState.EXECUTING -> Color(0xFFFFD58A)
        ReactorState.COMPLETED -> Color(0xFF8FFFD1)
        else -> Cyan
    }
    val accent by animateColorAsState(stateColor, tween(if (animate) 240 else 0), label = "state accent")
    val positions = listOf(.02f to .06f, .68f to .06f, .00f to .29f, .70f to .29f,
        .00f to .65f, .70f to .65f, .02f to .88f, .68f to .88f)
    val accessibleLayout = LocalDensity.current.fontScale > 1.25f
    val active = state in setOf(ReactorState.THINKING, ReactorState.PLANNING, ReactorState.EXECUTING, ReactorState.SPEAKING)
    Column {
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = if (accessibleLayout) 240.dp else 360.dp, max = if (accessibleLayout) 300.dp else 440.dp).aspectRatio(1f)) {
        val width = maxWidth
        val height = maxHeight
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Connected JARVIS reactor visualization" }) {
            val c = Offset(size.width / 2, size.height / 2)
            repeat(13) { i ->
                val x = size.width * i / 12
                drawLine(Cyan.copy(alpha = .025f), Offset(x, 0f), Offset(x, size.height))
                val y = size.height * i / 12
                drawLine(Cyan.copy(alpha = .025f), Offset(0f, y), Offset(size.width, y))
            }
            val radius = size.minDimension * .225f
            val angle = rotation.value
            val pulse = if (state == ReactorState.LISTENING) level * .10f else 0f
            val glowRadius = radius * (1.7f + pulse)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .15f), Color.Transparent), c, glowRadius), glowRadius, c)
            // Static fine circuitry connects all eight actual touch controls to the core.
            positions.forEachIndexed { index, (x, y) ->
                val end = Offset(size.width * (x + .15f), size.height * (y + .06f))
                val bend = Offset((c.x + end.x) / 2, end.y)
                val path = Path().apply { moveTo(c.x, c.y); lineTo(bend.x, bend.y); lineTo(end.x, end.y) }
                drawPath(path, accent.copy(alpha = .24f), style = Stroke(1.dp.toPx()))
                drawCircle(accent.copy(alpha = .7f), 2.dp.toPx(), end)
                if (animate && active) {
                    val progress = ((angle / 360f * 4 + index / 8f) % 1f)
                    val firstLength = (bend - c).getDistance()
                    val secondLength = (end - bend).getDistance()
                    val distance = progress * (firstLength + secondLength)
                    val point = if (distance <= firstLength) c + (bend - c) * (distance / firstLength.coerceAtLeast(1f))
                        else bend + (end - bend) * ((distance - firstLength) / secondLength.coerceAtLeast(1f))
                    drawCircle(accent.copy(alpha = .5f), 1.5.dp.toPx(), point)
                }
            }
            for (ring in 0..3) {
                val r = radius * (1f + ring * .14f + pulse)
                drawCircle(accent.copy(alpha = if (ring == 0) .65f else .13f), r, c, style = Stroke(if (ring == 0) 2.dp.toPx() else 1.dp.toPx()))
            }
            rotate(if (state == ReactorState.THINKING) -angle * 3 else angle, c) {
                repeat(72) { tick ->
                    val a = tick * PI / 36
                    val r = radius * 1.34f
                    val length = if (tick % 6 == 0) 7.dp.toPx() else 3.dp.toPx()
                    drawLine(accent.copy(alpha = if (tick % 6 == 0) .7f else .22f),
                        c + Offset(cos(a).toFloat() * r, sin(a).toFloat() * r),
                        c + Offset(cos(a).toFloat() * (r + length), sin(a).toFloat() * (r + length)), 1.dp.toPx())
                }
                repeat(3) { arc ->
                    val r = radius * (1.1f + arc * .16f)
                    drawArc(accent.copy(alpha = .75f - arc * .15f), arc * 125f, 62f, false,
                        c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            rotate(-angle * 1.5f, c) {
                repeat(40) { particle ->
                    val a = particle * 2.399963f
                    val r = radius * .82f * sqrt((particle + 1f) / 40f)
                    drawCircle(accent.copy(alpha = .2f + (particle % 4) * .1f), 1.dp.toPx(),
                        c + Offset(cos(a) * r, sin(a) * r))
                }
            }
            rotate(angle * .3f, c) {
                val orbital = radius * 1.85f
                drawOval(accent.copy(alpha = .09f), c - Offset(orbital, orbital * .58f),
                    Size(orbital * 2, orbital * 1.16f), style = Stroke(1.dp.toPx()))
            }
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .24f), Ink.copy(alpha = .92f)), c, radius * .83f), radius * .83f, c)
        }
        if (!accessibleLayout) Module.entries.forEachIndexed { index, module ->
            val (x, y) = positions[index]
            Column(Modifier.offset(width * x, height * y).width(width * .30f).heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = { onModule(module) })
                .semantics { contentDescription = "Open ${module.name.lowercase()} module" }
                .background(Brush.horizontalGradient(listOf(Panel.copy(alpha = .6f), Color.Transparent)))
                .padding(vertical = 7.dp, horizontal = 4.dp),
                horizontalAlignment = if (index % 2 == 0) Alignment.Start else Alignment.End) {
                Text("${module.code}  /  NODE", color = Muted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(module.name, color = Color(0xFFDCF9FF), fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp, letterSpacing = .3.sp, maxLines = 2)
                Text(if (module == Module.RESEARCH) "HANDOFF" else "LOCAL", color = Cyan.copy(alpha = .7f), fontSize = 8.sp)
            }
        }
        Column(Modifier.align(Alignment.Center).size(width * .37f)
            .clickable(role = Role.Button, onClick = onListen)
            .semantics { contentDescription = if (state == ReactorState.LISTENING) "Cancel listening" else "Activate voice command"; stateDescription = state.name }
            .padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J A A R V I S", color = Color.White, fontWeight = FontWeight.Light, fontSize = 15.sp)
            Spacer(Modifier.height(7.dp))
            Text(state.name, color = accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(if (state == ReactorState.LISTENING) "TAP TO CANCEL" else "TAP TO SPEAK", color = Muted, fontSize = 7.sp)
        }
    }
    if (accessibleLayout) Module.entries.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { module ->
                androidx.compose.material3.TextButton(onClick = { onModule(module) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        .semantics { contentDescription = "Open ${module.name.lowercase()} module" }) {
                    Text(module.name, color = Cyan, fontSize = 12.sp)
                }
            }
        }
    }
    }
}
