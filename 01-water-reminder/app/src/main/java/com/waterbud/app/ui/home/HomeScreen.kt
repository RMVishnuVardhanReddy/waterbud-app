package com.waterbud.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterbud.app.data.Cup
import com.waterbud.app.data.DrinkTypes
import com.waterbud.app.data.WaterLog
import com.waterbud.app.ui.components.WaterRing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCustom by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Waterbud") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            WaterRing(progress = state.progress) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.totalMl}",
                        fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("of ${state.goalMl} ml", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.weatherBonusMl > 0) {
                        Text("☀️ +${state.weatherBonusMl} ml for hot weather",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    if (state.goalReached) {
                        // Over-goal is still counted and shown (fixes "extra water isn't counted").
                        AssistChip(onClick = {}, label = { Text("${state.percent}% · goal reached 🎉") })
                    } else {
                        Text(
                            "${state.remainingMl} ml to go",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Quick add", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showCustom = true }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Custom")
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.cups, key = { it.id }) { cup ->
                    CupButton(cup) {
                        vm.addDrink(cup.amountMl, cup.drinkType)
                        scope.launch { snackbar.showSnackbar("+${cup.amountMl} ml") }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            ElevatedButton(
                onClick = { vm.addDrink(250) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Drink a glass (250 ml)", fontSize = 16.sp)
            }

            // Today's log — lets users review and fix mistakes (a common review ask).
            if (state.todayLogs.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Today's log", Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                state.todayLogs.forEach { log -> LogRow(log) { vm.undo(log) } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCustom) {
        CustomAmountDialog(
            onDismiss = { showCustom = false },
            onConfirm = { ml, type ->
                vm.addDrink(ml, type)
                showCustom = false
                scope.launch { snackbar.showSnackbar("+$ml ml") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomAmountDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("water") }
    val amount = text.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a drink") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter { it.isDigit() }.take(5) },
                    label = { Text("Amount (ml)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                Text("Drink type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrinkTypes.all.forEach { d ->
                        FilterChip(
                            selected = type == d.id,
                            onClick = { type = d.id },
                            label = { Text(d.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = amount in 1..5000, onClick = { onConfirm(amount, type) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun LogRow(log: WaterLog, onDelete: () -> Unit) {
    val time = remember(log.timestamp) {
        Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${log.amountMl} ml · ${DrinkTypes.label(log.drinkType)}",
                fontWeight = FontWeight.Medium)
            Text(time, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Delete entry")
        }
    }
}

@Composable
private fun CupButton(cup: Cup, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = 92.dp, height = 104.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.WaterDrop, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(cup.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text("${cup.amountMl} ml", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
