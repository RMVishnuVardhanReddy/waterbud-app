package com.breathe.calm

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Drives [BreathingEngine] from a frame clock and animates the circle.
 * Session runs for [sessionSeconds]; calls [onFinish] when done.
 */
@Composable
fun BreatheScreen(
    pattern: BreathPattern,
    sessionSeconds: Int,
    onFinish: () -> Unit
) {
    val engine = remember(pattern) { BreathingEngine(pattern) }
    var elapsed by remember { mutableDoubleStateOf(0.0) }

    // Advance a real-time clock ~60fps.
    LaunchedEffect(pattern) {
        val start = withFrameNanos { it }
        var last = start
        while (elapsed < sessionSeconds) {
            val now = withFrameNanos { it }
            elapsed += (now - last) / 1_000_000_000.0
            last = now
        }
        onFinish()
    }

    val frame = engine.frameAt(elapsed)
    val animatedScale by animateFloatAsState(
        targetValue = frame.scale,
        animationSpec = tween(300, easing = LinearEasing),
        label = "scale"
    )
    val remaining = (sessionSeconds - elapsed).toInt().coerceAtLeast(0)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text(pattern.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("%d:%02d left".format(remaining / 60, remaining % 60),
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                val c1 = MaterialTheme.colorScheme.secondary
                val c2 = MaterialTheme.colorScheme.primary
                Canvas(Modifier.size(300.dp)) {
                    val r = size.minDimension / 2 * animatedScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(c1.copy(alpha = 0.9f), c2.copy(alpha = 0.5f)),
                            radius = r
                        ),
                        radius = r, center = center
                    )
                }
                Text(frame.phase.label, fontSize = 24.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))

            Text("${frame.secondsLeftInPhase}", fontSize = 40.sp, fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
        }
    }
}
