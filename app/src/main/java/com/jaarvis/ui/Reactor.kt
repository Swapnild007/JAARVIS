package com.jaarvis.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaarvis.core.ReactorState
import kotlin.math.*

val Cyan = Color(0xFF64F4FF)
val Gold = Color(0xFFF5C97D)
val Muted = Color(0xFF8DAEBE)
val Panel = Color(0xFF071C2D)
val Ink = Color(0xFF020A13)

enum class Module(val subtitle: String, val code: String) {
    INTELLIGENCE("Local command routing", "01"), OPERATIONS("Device actions", "02"),
    RESEARCH("Search handoff", "03"), MEMORY("Private local notes", "04"),
    ENGINEERING("Execution diagnostics", "05"), FINANCE("Budget calculator", "06"),
    SYSTEM("Device & preferences", "07"), AUTOMATIONS("Clock alarms & timers", "08")
}

fun reactorAccent(state: ReactorState) = when (state) {
    ReactorState.ERROR -> Color(0xFFFF8F87)
    ReactorState.THINKING -> Color(0xFFC5B4FF)
    ReactorState.PLANNING -> Color(0xFF93BDFF)
    ReactorState.EXECUTING -> Gold
    ReactorState.COMPLETED -> Color(0xFF8FFFD1)
    else -> Cyan
}

/** One lifecycle-gated clock. Draw work is bounded, deterministic and independent of frame history. */
@Composable
fun ReactorMap(state: ReactorState, level: Float, motion: Float, animate: Boolean,
               onListen: () -> Unit, onModule: (Module) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.25f
    val running = state in setOf(ReactorState.LISTENING, ReactorState.THINKING, ReactorState.PLANNING, ReactorState.EXECUTING, ReactorState.SPEAKING)
    val angle: State<Float> = if (animate && motion > 0f) {
        rememberInfiniteTransition(label = "reactor orbit").animateFloat(0f, 360f,
            infiniteRepeatable(tween((26000 / motion.coerceAtLeast(.2f)).toInt(), easing = LinearEasing)), label = "phase")
    } else remember { mutableFloatStateOf(0f) }
    val accent by animateColorAsState(reactorAccent(state), tween(if (animate) 220 else 0), label = "reactor state")
    Column {
        Box(Modifier.fillMaxWidth().height(if (largeText) 230.dp else 260.dp)) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Cinematic layered reactor with orbital energy rings" }) {
                drawReactor(state, if (animate) level else 0f, angle.value, accent, running)
            }
            // The actual microphone control is part of the core, not a decorative fake control.
            Column(Modifier.align(Alignment.Center).offset(y = (-8).dp).size(108.dp)
                .clickable(role = Role.Button, onClick = onListen)
                .semantics { contentDescription = if (state == ReactorState.LISTENING) "Cancel listening" else "Activate voice command"; stateDescription = state.name },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("J", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraLight, fontFamily = FontFamily.Monospace)
                Text(if (state == ReactorState.LISTENING) "STOP MIC" else "ACTIVATE", color = accent, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(4.dp).background(accent, RoundedCornerShape(50)))
                Text(state.name, color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                Text("/  ${if (running) "ACTIVE" else "STANDBY"}", color = Muted, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SUBSYSTEM ARRAY", color = Gold, fontSize = 9.sp, letterSpacing = 2.sp)
            Text("08  /  LOCAL TOOLS", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(9.dp))
        Box {
            Canvas(Modifier.matchParentSize()) {
                val mid = size.width / 2
                drawLine(accent.copy(alpha = .35f), Offset(mid, 0f), Offset(mid, size.height), 1.dp.toPx())
                repeat(4) { row ->
                    val y = (row + .5f) * size.height / 4
                    drawLine(accent.copy(alpha = .24f), Offset(size.width * .25f, y), Offset(size.width * .75f, y), 1.dp.toPx())
                    drawCircle(accent, 2.dp.toPx(), Offset(mid, y))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Module.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { module -> SubsystemControl(module, accent, Modifier.weight(1f), onModule) }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawReactor(state: ReactorState, level: Float, angle: Float, accent: Color, running: Boolean) {
    val c = Offset(size.width / 2, size.height * .45f)
    val r = min(size.width * .37f, size.height * .34f)
    val velocity = when (state) { ReactorState.THINKING -> 2.6f; ReactorState.PLANNING -> -1.6f; ReactorState.EXECUTING -> 2f; ReactorState.SPEAKING -> 1.5f; else -> 1f }
    val phase = angle * velocity
    val pulse = if (state == ReactorState.LISTENING) level * .08f else 0f
    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .24f), accent.copy(alpha = .04f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
    // Projection meridians and circuitry extend the reactor into the whole viewport.
    repeat(5) { i ->
        val y = c.y + (i - 2) * r * .36f
        drawLine(accent.copy(alpha = .06f), Offset(0f, y), Offset(size.width, y), 1f)
    }
    for (direction in listOf(-1f, 1f)) {
        val start = c + Offset(direction * r * 1.12f, -r * .44f)
        val end = Offset(if (direction < 0) 4.dp.toPx() else size.width - 4.dp.toPx(), c.y - r * .8f)
        val p = Path().apply { moveTo(start.x, start.y); lineTo(end.x - direction * 16.dp.toPx(), start.y); lineTo(end.x, end.y) }
        drawPath(p, accent.copy(alpha = .36f), style = Stroke(1.dp.toPx()))
        drawCircle(accent.copy(alpha = .8f), 2.dp.toPx(), end)
    }
    repeat(3) { i ->
        rotate((if (i % 2 == 0) 1 else -1) * phase * .3f + i * 58f, c) {
            val wide = r * (1.35f + i * .06f)
            drawOval(accent.copy(alpha = .10f), c - Offset(wide, r * .57f), Size(wide * 2, r * 1.14f), style = Stroke(1.dp.toPx()))
        }
    }
    // A luminous core is painted BEFORE the particles, so it cannot conceal them.
    drawCircle(Brush.radialGradient(listOf(Color(0xFF123F50), Ink), c, r * .79f), r * .79f, c)
    repeat(110) { i ->
        val a = i * 2.3999632f + phase * .009f
        val d = r * .72f * sqrt((i + .5f) / 110f)
        val position = c + Offset(cos(a) * d, sin(a) * d)
        drawCircle(accent.copy(alpha = .18f + (i % 5) * .08f), if (i % 7 == 0) 1.3.dp.toPx() else .65.dp.toPx(), position)
    }
    repeat(3) { vane ->
        rotate(vane * 120f - phase * .5f, c) {
            val p = Path().apply {
                moveTo(c.x - r * .16f, c.y - r * .75f); lineTo(c.x + r * .13f, c.y - r * .75f)
                lineTo(c.x + r * .36f, c.y - r * .28f); lineTo(c.x + r * .11f, c.y - r * .14f); close()
            }
            drawPath(p, Brush.linearGradient(listOf(accent.copy(alpha = .5f), accent.copy(alpha = .03f))))
            drawPath(p, accent.copy(alpha = .7f), style = Stroke(.7.dp.toPx()))
        }
    }
    for (i in 0..7) {
        val radius = r * (.78f + i * .055f + pulse)
        drawCircle(accent.copy(alpha = if (i == 0 || i == 5) .55f else .11f), radius, c, style = Stroke(if (i == 5) 2.dp.toPx() else .7.dp.toPx()))
    }
    rotate(phase, c) {
        repeat(120) { tick ->
            val a = tick * PI / 60
            val major = tick % 10 == 0
            val start = r * 1.22f
            drawLine(accent.copy(alpha = if (major) .85f else .25f),
                c + Offset(cos(a).toFloat() * start, sin(a).toFloat() * start),
                c + Offset(cos(a).toFloat() * (start + if (major) 7.dp.toPx() else 2.dp.toPx()), sin(a).toFloat() * (start + if (major) 7.dp.toPx() else 2.dp.toPx())), .7.dp.toPx())
        }
    }
    repeat(5) { i ->
        val radius = r * (.87f + i * .078f)
        val start = (if (i % 2 == 0) phase else -phase) + i * 73f
        val sweep = if (i % 2 == 0) 66f else 32f
        val color = if (i == 2) Gold else accent
        for ((width, opacity) in listOf(14f to .035f, 8f to .10f, 3f to .85f)) {
            drawArc(color.copy(alpha = opacity), start, sweep, false, c - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(width.dp.toPx(), cap = StrokeCap.Round))
        }
    }
    repeat(8) { i ->
        val a = (phase * .7f + i * 45f) * PI / 180
        val position = c + Offset(cos(a).toFloat() * r * 1.36f, sin(a).toFloat() * r * 1.12f)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .5f), Color.Transparent), position, 8.dp.toPx()), 8.dp.toPx(), position)
        drawCircle(Color.White.copy(alpha = .8f), 1.7.dp.toPx(), position)
    }
    if (state == ReactorState.LISTENING || state == ReactorState.SPEAKING) {
        repeat(25) { i ->
            // Speaking bars indicate engine state, not sampled TTS amplitude.
            val measured = if (state == ReactorState.LISTENING) level else .24f
            val envelope = sin((i / 24f) * PI).toFloat()
            val h = 2.dp.toPx() + measured * envelope * 14.dp.toPx()
            val x = c.x + (i - 12) * 4.dp.toPx()
            drawLine(accent.copy(alpha = .8f), Offset(x, c.y + r * 1.12f - h), Offset(x, c.y + r * 1.12f + h), 1.5.dp.toPx())
        }
    }
    val bottom = c + Offset(0f, r * 1.24f)
    drawLine(accent.copy(alpha = if (running) .7f else .22f), bottom, Offset(c.x, size.height - 21.dp.toPx()), 1.dp.toPx())
    if (running) {
        val y = bottom.y + (((phase / 90f) % 1f + 1f) % 1f) * (size.height - 21.dp.toPx() - bottom.y)
        drawCircle(accent, 2.dp.toPx(), Offset(c.x, y))
    }
}

@Composable
private fun SubsystemControl(module: Module, accent: Color, modifier: Modifier, open: (Module) -> Unit) {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    Row(modifier.heightIn(min = 48.dp).border(1.dp, accent.copy(alpha = .28f), shape)
        .background(Brush.linearGradient(listOf(Color(0xFF0A283A), Color(0xFF04111E))), shape)
        .clickable(role = Role.Button, onClick = { open(module) })
        .semantics { contentDescription = "Open ${module.name.lowercase()} module" }
        .padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModuleGlyph(module, accent, Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(module.name, color = Color(0xFFE5F9FF), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = .4.sp, maxLines = 2)
            Text("${module.code}  /  ${if (module == Module.RESEARCH) "BROWSER" else "LOCAL"}", color = Muted, fontSize = 8.sp, lineHeight = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ModuleGlyph(module: Module, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val c = center; val r = size.minDimension * .35f; val w = 1.3.dp.toPx()
        when (module) {
            Module.INTELLIGENCE -> { drawCircle(color, r * .45f, c, style = Stroke(w)); repeat(6) { val a = it * PI / 3; val p = c + Offset(cos(a).toFloat() * r, sin(a).toFloat() * r); drawLine(color.copy(alpha = .5f), c, p, w); drawCircle(color, w, p) } }
            Module.OPERATIONS -> repeat(4) { val x = if (it % 2 == 0) -r else r * .2f; val y = if (it < 2) -r else r * .2f; drawRect(color, c + Offset(x, y), Size(r * .8f, r * .8f), style = Stroke(w)) }
            Module.RESEARCH -> { drawCircle(color, r * .75f, c - Offset(r * .2f, r * .2f), style = Stroke(w)); drawLine(color, c + Offset(r * .4f, r * .4f), c + Offset(r, r), w) }
            Module.MEMORY -> repeat(3) { val y = c.y + (it - 1) * r * .7f; drawLine(color, Offset(c.x - r, y), Offset(c.x + r, y), w); drawCircle(color, w, Offset(c.x - r, y)) }
            Module.ENGINEERING -> { val p = Path().apply { moveTo(c.x - r * .4f,c.y-r); lineTo(c.x-r,c.y); lineTo(c.x-r*.4f,c.y+r); moveTo(c.x+r*.4f,c.y-r); lineTo(c.x+r,c.y); lineTo(c.x+r*.4f,c.y+r) }; drawPath(p,color,style=Stroke(w)) }
            Module.FINANCE -> repeat(3) { drawLine(color, c + Offset((it - 1) * r * .7f, r), c + Offset((it - 1) * r * .7f, r - (it + 1) * r * .65f), w * 2) }
            Module.SYSTEM -> { drawCircle(color, r, c, style = Stroke(w)); drawCircle(color, r * .35f, c, style = Stroke(w)); repeat(4) { rotate(it * 90f, c) { drawLine(color, c + Offset(r * .8f,0f),c + Offset(r * 1.3f,0f),w) } } }
            Module.AUTOMATIONS -> { drawArc(color, 30f, 280f, false, c - Offset(r,r), Size(r*2,r*2), style = Stroke(w)); drawLine(color,c,c-Offset(0f,r*.65f),w); drawLine(color,c,c+Offset(r*.5f,0f),w) }
        }
    }
}
