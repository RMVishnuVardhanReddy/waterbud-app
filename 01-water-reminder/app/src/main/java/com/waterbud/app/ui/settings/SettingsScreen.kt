package com.waterbud.app.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterbud.app.R
import com.waterbud.app.ads.BannerAd
import kotlinx.coroutines.launch
import com.waterbud.app.data.Cup
import com.waterbud.app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onWatchRewardedForThemes: (onDone: (Boolean) -> Unit) -> Unit,
    onEnableHealthSync: (onResult: (Boolean) -> Unit) -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val s by vm.uiState.collectAsStateWithLifecycle()
    val cups by vm.cups.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddCup by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.setWeatherAdjust(true)
        else scope.launch { snackbar.showSnackbar("Location is needed to adjust the goal for weather") }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportBackup(uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "Backup saved" else "Backup failed") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importBackup(uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "Backup restored" else "Couldn't read that backup") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BannerAd(isPremium = s.isPremium) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (!s.isPremium) {
                PremiumCard(onUpgrade)
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle("Daily goal")
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${s.goalMl} ml", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Slider(
                        value = s.goalMl.toFloat(),
                        onValueChange = { vm.setGoal((it / 50).toInt() * 50) },
                        valueRange = 1000f..5000f
                    )
                    Text("Tip: recommended is ~35 ml per kg of body weight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.goal_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                    if (s.weatherBonusMl > 0) {
                        Text("☀️ Hot today: +${s.weatherBonusMl} ml → goal ${s.effectiveGoalMl} ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Adjust goal on hot days")
                            Text("Uses your approximate location + local temperature.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = s.weatherAdjustEnabled,
                            onCheckedChange = { want ->
                                if (want) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) vm.setWeatherAdjust(true)
                                    else locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else vm.setWeatherAdjust(false)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.health_note_title))
            Card(shape = RoundedCornerShape(16.dp)) {
                Text(
                    stringResource(R.string.health_note_body),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Reminders")
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SwitchRow("Enable reminders", s.remindersEnabled, vm::setRemindersEnabled)
                    HorizontalDivider()
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("Reminder interval: every ${s.intervalMinutes} min")
                        Slider(
                            value = s.intervalMinutes.toFloat(),
                            onValueChange = { vm.setInterval((it / 15).toInt() * 15) },
                            valueRange = 30f..240f, enabled = s.remindersEnabled
                        )
                    }
                    HorizontalDivider()
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("Quiet hours: ${two(s.quietStartHour)}:00 – ${two(s.quietEndHour)}:00")
                        Text("No reminders while you sleep. Reminders respect Do Not Disturb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Start: ${two(s.quietStartHour)}:00")
                        Slider(
                            value = s.quietStartHour.toFloat(),
                            onValueChange = { vm.setQuietHours(it.toInt(), s.quietEndHour) },
                            valueRange = 18f..23f, enabled = s.remindersEnabled
                        )
                        Text("End: ${two(s.quietEndHour)}:00")
                        Slider(
                            value = s.quietEndHour.toFloat(),
                            onValueChange = { vm.setQuietHours(s.quietStartHour, it.toInt()) },
                            valueRange = 5f..10f, enabled = s.remindersEnabled
                        )
                    }
                    HorizontalDivider()
                    SwitchRow("Pause when goal reached", s.pauseWhenGoalReached,
                        vm::setPauseWhenGoalReached)
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Alert during Do Not Disturb")
                            Text("Off by default. Turn on if you want reminders to break through DnD/silent — you'll grant one system permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = s.overrideDnd,
                            enabled = s.remindersEnabled,
                            onCheckedChange = { want ->
                                vm.setOverrideDnd(want)
                                if (want) {
                                    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                                            as android.app.NotificationManager
                                    if (!nm.isNotificationPolicyAccessGranted) {
                                        context.startActivity(
                                            Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                        scope.launch {
                                            snackbar.showSnackbar("Grant Waterbud 'Do Not Disturb access' to finish enabling this")
                                        }
                                    }
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    SwitchRow("Silent reminders (no sound)", s.silentReminders, vm::setSilentReminders)
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Different weekend hours")
                            Text("Sleep in on Sat/Sun — reminders start later.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = s.weekendDifferent, enabled = s.remindersEnabled,
                            onCheckedChange = vm::setWeekendDifferent)
                    }
                    if (s.weekendDifferent) {
                        Column(Modifier.padding(bottom = 12.dp)) {
                            Text("Weekend quiet: ${two(s.weekendQuietStartHour)}:00 – ${two(s.weekendQuietEndHour)}:00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Start: ${two(s.weekendQuietStartHour)}:00")
                            Slider(value = s.weekendQuietStartHour.toFloat(),
                                onValueChange = { vm.setWeekendQuietHours(it.toInt(), s.weekendQuietEndHour) },
                                valueRange = 18f..23f, enabled = s.remindersEnabled)
                            Text("End: ${two(s.weekendQuietEndHour)}:00")
                            Slider(value = s.weekendQuietEndHour.toFloat(),
                                onValueChange = { vm.setWeekendQuietHours(s.weekendQuietStartHour, it.toInt()) },
                                valueRange = 6f..12f, enabled = s.remindersEnabled)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Quick-add cups")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAddCup = true }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Add")
                }
            }
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (cups.isEmpty()) {
                        Text("No cups yet — add your favourite sizes.",
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    cups.forEachIndexed { i, cup ->
                        if (i > 0) HorizontalDivider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${cup.label} · ${cup.amountMl} ml", Modifier.weight(1f))
                            IconButton(onClick = { vm.deleteCup(cup) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete cup")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Appearance")
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val locked = mode == ThemeMode.AMOLED && !s.themesUnlocked
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable(enabled = !locked) { vm.setTheme(mode) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = s.theme == mode, onClick = { if (!locked) vm.setTheme(mode) })
                            Spacer(Modifier.width(8.dp))
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                            Spacer(Modifier.weight(1f))
                            if (locked) {
                                TextButton(onClick = {
                                    onWatchRewardedForThemes { ok ->
                                        scope.launch {
                                            snackbar.showSnackbar(
                                                if (ok) "Themes unlocked for today 🎉" else "No ad available right now"
                                            )
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.PlayCircle, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Watch ad")
                                }
                                AssistChip(onClick = onUpgrade, label = { Text("Premium") })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Data")
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable {
                            if (!s.isPremium) onUpgrade()
                            else vm.exportCsv { uri ->
                                if (uri == null) {
                                    scope.launch { snackbar.showSnackbar("Nothing to export yet") }
                                } else {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Export CSV"))
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Export my data (CSV)", fontWeight = FontWeight.Medium)
                        Text("Your full hydration history, yours to keep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!s.isPremium) AssistChip(onClick = onUpgrade, label = { Text("Premium") })
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sync to Health Connect", fontWeight = FontWeight.Medium)
                        Text("Mirror your water into the phone's health hub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = s.healthSyncEnabled,
                        onCheckedChange = { want ->
                            if (want) onEnableHealthSync { ok ->
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (ok) "Health Connect sync on"
                                        else "Health Connect isn't available or permission was denied"
                                    )
                                }
                            } else vm.setHealthSyncEnabled(false)
                        }
                    )
                }
                HorizontalDivider()
                DataActionRow("Back up to a file", "Save all data as JSON — keep it in Drive or anywhere.") {
                    backupLauncher.launch("waterbud-backup.json")
                }
                HorizontalDivider()
                DataActionRow("Restore from a file", "Replace current data with a chosen backup.") {
                    restoreLauncher.launch(arrayOf("application/json"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddCup) {
        AddCupDialog(
            onDismiss = { showAddCup = false },
            onConfirm = { label, ml -> vm.addCup(label, ml); showAddCup = false }
        )
    }
}

@Composable
private fun AddCupDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val ml = amount.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a cup") },
        text = {
            Column {
                OutlinedTextField(label, { label = it.take(16) }, label = { Text("Name (e.g. Bottle)") },
                    singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    amount, { s -> amount = s.filter { it.isDigit() }.take(5) },
                    label = { Text("Amount (ml)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { TextButton(enabled = ml in 1..5000, onClick = { onConfirm(label, ml) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable private fun DataActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable private fun PremiumCard(onUpgrade: () -> Unit) {
    Card(
        onClick = onUpgrade,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Go Premium", fontWeight = FontWeight.Bold)
                Text("Remove ads · all themes · data export · cloud backup",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun two(h: Int) = h.toString().padStart(2, '0')
