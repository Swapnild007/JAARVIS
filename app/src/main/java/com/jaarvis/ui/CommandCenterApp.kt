package com.jaarvis.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaarvis.CommandCenterModel
import com.jaarvis.core.ReactorState
import com.jaarvis.core.LocalApp
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun CommandCenterApp(model: CommandCenterModel) {
    val context = LocalContext.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_STOP && (context as? android.app.Activity)?.isChangingConfigurations != true) model.onBackground()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var screen by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.listen() else model.fail("Microphone access was denied. Type a command, or enable microphone access in Android app settings.")
    }
    val listen = {
        if (model.state == ReactorState.LISTENING) model.cancel()
        else if (!model.speechAvailable) model.fail("No Android speech service is installed. Use the keyboard.")
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) model.listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    BackHandler(enabled = screen != null || model.state == ReactorState.LISTENING) {
        if (model.state == ReactorState.LISTENING) model.cancel() else screen = null
    }
    val navy = if (model.preferences.theme == "Deep blue") Color(0xFF071E35) else Ink
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = navy, surface = Panel,
        onBackground = Color(0xFFE6F5FF), onSurface = Color(0xFFE6F5FF), onPrimary = Ink,
        secondary = Muted, error = Color(0xFFFFB4A5))) {
        Surface(Modifier.fillMaxSize(), color = navy) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(navy, Color(0xFF07182B), navy)))
                .safeDrawingPadding().imePadding()) {
                if (!imeVisible || screen != null) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    if (screen != null) TextButton(onClick = { screen = null }) { Text("‹  BACK") }
                    else Text("J / C", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("JAARVIS", fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
                        Text("COMMAND CENTER  /  01", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
                HorizontalDivider(color = Cyan.copy(alpha = .15f))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (screen == null) {
                        Home(model, resumed, listen) { screen = it.name }
                    } else ModuleScreen(Module.valueOf(screen!!), model)
                    Spacer(Modifier.height(20.dp))
                }
                CommandDock(model, listen, imeVisible)
            }
        }
    }
}

@Composable
private fun Home(model: CommandCenterModel, resumed: Boolean, listen: () -> Unit, open: (Module) -> Unit) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(resumed) { if (resumed) while (true) { now = ZonedDateTime.now(); delay(1000L * (60 - now.second)) } }
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 30.sp, fontWeight = FontWeight.Light)
            Text(now.format(DateTimeFormatter.ofPattern("EEE, dd MMM  •  z")), fontSize = 10.sp, color = Muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("LOCAL CORE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.sp)
            Text("BATTERY  ${model.battery?.let { "$it%" } ?: "UNAVAILABLE"}", fontSize = 10.sp, color = Muted)
            Text("NO CLOUD CONNECTION", color = Muted, fontSize = 8.sp)
        }
    }
    val context = LocalContext.current
    val systemMotion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    ReactorMap(model.state, if (model.preferences.reducedMotion) 0f else model.level, model.preferences.motion,
        resumed && !model.preferences.reducedMotion && systemMotion, listen, open)
    Text("ONE CORE. EIGHT CONNECTIONS.", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp,
        modifier = Modifier.alignCenter())
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { model.submit("time") }, modifier = Modifier.weight(1f)) { Text("TIME", fontSize = 11.sp) }
        OutlinedButton(onClick = { model.submit("battery") }, modifier = Modifier.weight(1f)) { Text("BATTERY", fontSize = 11.sp) }
        OutlinedButton(onClick = { open(Module.SYSTEM) }, modifier = Modifier.weight(1f)) { Text("PREFS", fontSize = 11.sp) }
    }
}

private fun Modifier.alignCenter() = this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)

@Composable
private fun CommandDock(model: CommandCenterModel, listen: () -> Unit, imeVisible: Boolean) {
    var command by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() { if (command.isNotBlank()) { model.submit(command); command = ""; keyboard?.hide() } }
    Column(Modifier.fillMaxWidth().background(Panel.copy(alpha = .95f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
        if (!imeVisible) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("●  ${model.state.name}", color = if (model.state == ReactorState.ERROR) MaterialTheme.colorScheme.error else Cyan,
                fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            if (model.state != ReactorState.IDLE) TextButton(onClick = model::cancel, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (model.state in setOf(ReactorState.COMPLETED, ReactorState.ERROR)) "RESET" else "STOP", fontSize = 10.sp) }
            TextButton(onClick = listen, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (model.state == ReactorState.LISTENING) "STOP MIC" else "VOICE", fontSize = 10.sp) }
        }
        if (!imeVisible) Text(model.status, color = if (model.state == ReactorState.ERROR) MaterialTheme.colorScheme.error else Muted,
            fontSize = 12.sp, lineHeight = 17.sp, maxLines = 4,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = command, onValueChange = { command = it.take(500) }, label = { Text("Enter a local command") },
                modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { submit() }))
            Button(onClick = { submit() }, enabled = command.isNotBlank(), contentPadding = PaddingValues(12.dp),
                modifier = Modifier.sizeIn(minWidth = 52.dp, minHeight = 52.dp).semantics { contentDescription = "Send command" }) { Text("GO") }
        }
    }
}

@Composable
private fun ModuleScreen(module: Module, model: CommandCenterModel) {
    Spacer(Modifier.height(24.dp))
    Text("${module.code}  /  CONNECTED MODULE", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
    Text(module.name, fontSize = 26.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 8.dp))
    Text(module.subtitle, color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(24.dp))
    when (module) {
        Module.INTELLIGENCE -> {
            Info("Local intelligence", "One bounded local core handles device readings, app launches, Maps and Clock handoffs. General AI is not connected. These are local rules, not an autonomous AI planner.")
            Action("Ask for the current date") { model.submit("date") }
            Info("Command history", "The latest 30 commands stay on this device. VERIFIED means a device reading returned; HANDOFF means Android accepted an intent, not that the other app completed a task.")
            if (model.history.isEmpty()) Text("No saved commands.", color = Muted)
            model.history.forEach { record -> Info(record.command, "${record.outcome} · ${java.time.Instant.ofEpochMilli(record.timestamp).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))}\n${record.response}") }
            Action("Clear command history", action = model::clearHistory)
        }
        Module.OPERATIONS -> {
            Info("Device control", "Actions use Android’s supported intents. JAARVIS does not control arbitrary app screens.")
            Action("Open Android Settings") { model.submit("open settings") }
            Action("Open YouTube") { model.submit("open youtube") }
            LocalApp.entries.forEach { app -> Action("Open ${app.label}") { model.submit("open ${app.label.lowercase()}") } }
            Action("Read battery level") { model.submit("battery") }
        }
        Module.RESEARCH -> Research()
        Module.MEMORY -> {
            Info("On this device", "Your note is saved in JAARVIS app storage. Cloud sync and vector memory are not connected. Uninstalling clears the note; Android backup is disabled.")
            OutlinedTextField(value = model.note, onValueChange = { model.saveNote(it.take(10000)) },
                label = { Text("Local note · saved automatically") }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp))
            Action("Clear note") { model.saveNote("") }
        }
        Module.ENGINEERING -> {
            Info("Supported grammar", "time\ndate\nbattery\nopen settings / youtube / camera / maps / clock / calculator\nnavigate to Pune\nalarm 07:30\nset alarm at 7:30 pm\ntimer 5 minutes\ncancel")
            Info("Last command plan", model.activePlan.ifEmpty { "No active plan. Local commands produce one allowlisted step." })
            model.history.firstOrNull()?.let { Info("Execution trace", "${it.plan}\n${it.stages.joinToString(" → ")}") }
            Info("Native architecture", "Kotlin + Compose Canvas\nBounded local command router\nEvent-driven reactor states\nAndroid SpeechRecognizer + TextToSpeech\nNo server or API key required")
            Info("Integration boundary", "No code execution, app builder, shell access or remote device control is available in this release.")
        }
        Module.FINANCE -> Finance()
        Module.SYSTEM -> SystemPreferences(model)
        Module.AUTOMATIONS -> {
            Info("Clock handoff", "Enter ‘alarm 07:30’ below. Android Clock opens for review and saving. JAARVIS cannot verify that you saved the alarm.")
            Action("Review a 07:30 alarm in Clock") { model.submit("alarm 07:30") }
            Action("Review a 5 minute timer in Clock") { model.submit("timer 5 minutes") }
            Info("Reminder foundation", "Alarm and timer requests delegate to Android Clock. No JAARVIS notification scheduling, incoming calls, wake word, screen-off listening or two-way reminder voice is implemented.")
        }
    }
}

@Composable
private fun Research() {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    Info("External search", "A search opens in your browser after you tap the button. Your query is sent to DuckDuckGo. JAARVIS does not fetch, summarize or fabricate results.")
    OutlinedTextField(query, { query = it.take(500) }, label = { Text("Research query") }, modifier = Modifier.fillMaxWidth())
    Action("Search in browser", enabled = query.isNotBlank()) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=${Uri.encode(query)}")))
            status = "Query handed to your browser."
        } catch (_: android.content.ActivityNotFoundException) { status = "No browser is installed." }
        catch (_: SecurityException) { status = "Android blocked opening the browser." }
    }
    Text(status, color = Muted)
}

@Composable
private fun Finance() {
    var income by rememberSaveable { mutableStateOf("") }
    var expenses by rememberSaveable { mutableStateOf("") }
    Info("Local budget", "Subtract expenses from income in the same currency. Values stay in this screen’s saved state. No bank connection or live market data.")
    OutlinedTextField(income, { income = it.take(18) }, label = { Text("Income") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(expenses, { expenses = it.take(18) }, label = { Text("Expenses") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    val amount = Regex("^\\d{1,12}(?:\\.\\d{1,2})?$")
    val i = income.takeIf { amount.matches(it) }?.toBigDecimalOrNull()
    val e = expenses.takeIf { amount.matches(it) }?.toBigDecimalOrNull()
    val result = if (i != null && e != null && i >= BigDecimal.ZERO && e >= BigDecimal.ZERO) (i - e).stripTrailingZeros().toPlainString() else null
    Info("Remaining budget", result ?: "Enter two non-negative decimal amounts.")
}

@Composable
private fun SystemPreferences(model: CommandCenterModel) {
    val context = LocalContext.current
    val p = model.preferences
    Info("Android device", "${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nBattery ${model.battery?.let { "$it%" } ?: "unavailable"}\nSpeech service: ${if (model.onDeviceSpeech) "on-device available" else if (model.speechAvailable) "system service (may use network)" else "unavailable"}\nTTS engine: ${if (model.ttsReady) "ready" else "initializing or unavailable"}")
    Toggle("Spoken feedback", p.sound) { model.updatePreferences(p.copy(sound = it)) }
    Toggle("Reduced motion", p.reducedMotion) { model.updatePreferences(p.copy(reducedMotion = it)) }
    Text("Motion intensity", modifier = Modifier.padding(top = 16.dp))
    Slider(p.motion, { model.updatePreferences(p.copy(motion = it)) }, enabled = !p.reducedMotion,
        modifier = Modifier.semantics { contentDescription = "Motion intensity" })
    Text("Android’s animation-off setting also disables orbital motion. Animation stops in the background.", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(20.dp))
    Text("Theme", color = Cyan)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Midnight", "Deep blue").forEach { theme -> FilterChip(selected = p.theme == theme,
            onClick = { model.updatePreferences(p.copy(theme = theme)) }, label = { Text(theme) }) }
    }
    Info("Voice privacy", "Microphone is requested only when you activate voice. On Android 12+, on-device recognition is preferred when installed. Otherwise the Android speech service may use its network. Listening stops after 20 seconds without a result. Nothing listens continuously. TTS uses your installed engine and voice data.")
    Action("Open Android app permissions") {
        try {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        } catch (_: android.content.ActivityNotFoundException) { model.fail("App settings are unavailable on this device.") }
        catch (_: SecurityException) { model.fail("Android blocked opening app settings.") }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = change),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label); Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun Info(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).border(1.dp, Cyan.copy(alpha = .15f), RoundedCornerShape(12.dp))
        .background(Panel.copy(alpha = .7f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(title, color = Color(0xFFE6F5FF), fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Action(label: String, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 48.dp)) { Text(label) }
}
