package com.waterbud.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.compose.foundation.layout.Column
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear OS companion. The watch is a remote control for the phone app: tapping a
 * button sends a "log water" message to the phone (the single source of truth),
 * and the phone pushes today's total back over the Data Layer for display.
 *
 * Needs device/emulator testing with both apps installed and paired.
 */
class WearActivity : ComponentActivity() {

    private var total by mutableIntStateOf(0)
    private var goal by mutableIntStateOf(2000)

    private val dataListener = DataClient.OnDataChangedListener { events ->
        for (event in events) {
            if (event.dataItem.uri.path == PATH_TODAY) {
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                total = map.getInt("total", total)
                goal = map.getInt("goal", goal)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readInitialTotal()
        setContent {
            WaterWearScreen(total, goal, onAdd = ::logWater)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(dataListener)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(dataListener)
    }

    private fun logWater(ml: Int) {
        total += ml // optimistic; phone will confirm via Data Layer
        lifecycleScope.launch {
            runCatching {
                val nodes = Wearable.getNodeClient(this@WearActivity).connectedNodes.await()
                val mc = Wearable.getMessageClient(this@WearActivity)
                nodes.forEach { node ->
                    mc.sendMessage(node.id, PATH_LOG, ml.toString().toByteArray()).await()
                }
            }
        }
    }

    private fun readInitialTotal() {
        lifecycleScope.launch {
            runCatching {
                val items = Wearable.getDataClient(this@WearActivity).dataItems.await()
                items.forEach { item ->
                    if (item.uri.path == PATH_TODAY) {
                        val map = DataMapItem.fromDataItem(item).dataMap
                        total = map.getInt("total", 0); goal = map.getInt("goal", 2000)
                    }
                }
                items.release()
            }
        }
    }

    companion object {
        const val PATH_LOG = "/log_water"
        const val PATH_TODAY = "/today"
    }
}

@Suppress("unused")
@Composable
private fun WaterWearScreen(total: Int, goal: Int, onAdd: (Int) -> Unit) {
    MaterialTheme {
        Scaffold(timeText = { TimeText() }) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(28.dp))
                Text("💧 $total", fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = MaterialTheme.colors.primary)
                Text("of $goal ml", fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAdd(250) }) { Text("+250") }
                    Button(onClick = { onAdd(500) }) { Text("+500") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
