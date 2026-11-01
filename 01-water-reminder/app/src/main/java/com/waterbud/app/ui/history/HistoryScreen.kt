package com.waterbud.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterbud.app.R
import com.waterbud.app.ads.BannerAd
import com.waterbud.app.data.Achievement

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val achievements = state.achievements

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BannerAd(isPremium = state.isPremium) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Current streak",
                    value = pluralStringResource(R.plurals.days, state.currentStreak, state.currentStreak),
                    icon = { Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Weekly average",
                    value = "${state.weeklyAverageMl} ml"
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                state.days.forEach { day -> BarColumn(day, Modifier.weight(1f)) }
            }

            if (achievements.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                val earned = achievements.count { it.earned }
                Text("Achievements · $earned/${achievements.size}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    achievements.forEach { BadgeCard(it) }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(a: Achievement) {
    Card(
        modifier = Modifier.size(width = 104.dp, height = 118.dp).alpha(if (a.earned) 1f else 0.5f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (a.earned) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(a.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(a.title, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
            if (!a.earned && a.progress > 0f) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { a.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
        }
    }
}

@Composable
private fun BarColumn(day: DayBar, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (day.totalMl > 0) "${day.totalMl}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .height((140 * day.fraction).dp.coerceAtLeast(6.dp))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    if (day.metGoal) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(day.label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: (@Composable () -> Unit)? = null
) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
                Text(title, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
