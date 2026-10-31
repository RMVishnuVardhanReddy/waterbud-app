package com.waterbud.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Circular progress ring with a gradient sweep and an animated fill.
 * [progress] is 0f..1f. Center content is provided by [content].
 */
@Composable
fun WaterRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 240.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val start = MaterialTheme.colorScheme.secondary
    val end = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val sw = strokeWidth.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = track,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(start, end, start)),
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        content()
    }
}
