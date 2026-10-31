package com.waterbud.app.ui.onboarding

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waterbud.app.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterbud.app.data.GoalCalculator
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.reminder.ReminderScheduler
import kotlinx.coroutines.launch

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = PrefsRepository(app)
    fun finish(weightKg: Int, activity: Int, onDone: () -> Unit) = viewModelScope.launch {
        val goal = GoalCalculator.recommendedMl(weightKg, activity)
        prefs.completeOnboarding(goal, weightKg, activity)
        ReminderScheduler.reschedule(getApplication())
        onDone()
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    var weight by remember { mutableIntStateOf(70) }
    var activity by remember { mutableIntStateOf(1) }
    val goal = remember(weight, activity) { GoalCalculator.recommendedMl(weight, activity) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Default.WaterDrop, null, Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Welcome to Waterbud", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Let's set a goal that fits you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(40.dp))
        Text("Your weight: $weight kg", style = MaterialTheme.typography.titleMedium)
        Slider(value = weight.toFloat(), onValueChange = { weight = it.toInt() },
            valueRange = 30f..150f)

        Spacer(Modifier.height(24.dp))
        Text("Activity level", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        listOf("Low / sedentary", "Normal", "High / athlete").forEachIndexed { i, label ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(selected = activity == i, onClick = { activity = i })
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = activity == i, onClick = { activity = i })
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your daily goal", style = MaterialTheme.typography.labelLarge)
                Text("$goal ml", style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.goal_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { vm.finish(weight, activity, onDone) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Start tracking", style = MaterialTheme.typography.titleMedium) }
    }
}
