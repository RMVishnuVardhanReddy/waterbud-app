package com.focus.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal home: a clock, up to 6 favorites as plain text, and a drawer you
 * reveal to see everything with instant search. No icons, no clutter.
 */
@Composable
fun HomeLauncherScreen(
    time: String,
    date: String,
    favorites: List<AppInfo>,
    allApps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!drawerOpen) {
            Column(Modifier.fillMaxSize().padding(28.dp)) {
                Spacer(Modifier.height(48.dp))
                Text(time, fontSize = 56.sp, fontWeight = FontWeight.Light)
                Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.weight(1f))
                favorites.forEach { app ->
                    Text(
                        app.label,
                        fontSize = 22.sp, fontWeight = FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onLaunch(app) }
                            .padding(vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { drawerOpen = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("All apps ↑")
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(40.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Text(
                            app.label,
                            fontSize = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onLaunch(app); drawerOpen = false; query = "" }
                                .padding(vertical = 14.dp)
                        )
                    }
                }
                TextButton(onClick = { drawerOpen = false; query = "" },
                    modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Close ↓") }
            }
        }
    }
}
