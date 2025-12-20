package com.example.myapplication.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AuroraGreen
import com.example.myapplication.ui.theme.CosmicBlue
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.NebulaPink
import com.example.myapplication.ui.theme.NebulaPurple
import com.example.myapplication.ui.theme.StarWhite
import com.example.myapplication.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassVisualization(
    azimuth: Double,
    elevation: Double,
    satelliteName: String,
    modifier: Modifier = Modifier
) {
    // Animate azimuth changes
    val animatedAzimuth by animateFloatAsState(
        targetValue = azimuth.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "azimuth"
    )

    // Animate elevation changes
    val animatedElevation by animateFloatAsState(
        targetValue = elevation.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevation"
    )

    // Pulsing animation for satellite indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.85f

            // Draw outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NebulaPurple.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.2f
                ),
                center = center,
                radius = radius * 1.2f
            )

            // Draw compass circles
            listOf(1f, 0.66f, 0.33f).forEach { scale ->
                drawCircle(
                    color = GlassBorder,
                    radius = radius * scale,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // Draw cardinal directions
            val directions = listOf("N", "E", "S", "W")
            val angles = listOf(0f, 90f, 180f, 270f)

            directions.forEachIndexed { index, direction ->
                val angle = Math.toRadians(angles[index].toDouble())
                val textRadius = radius * 1.1f
                val x = center.x + textRadius * sin(angle).toFloat()
                val y = center.y - textRadius * cos(angle).toFloat()

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 48f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText(direction, x, y + 16f, paint)
                }
            }

            // Draw degree markers
            for (degree in 0 until 360 step 30) {
                val angle = Math.toRadians(degree.toDouble())
                val startRadius = if (degree % 90 == 0) radius * 0.9f else radius * 0.95f
                val endRadius = radius

                val startX = center.x + startRadius * sin(angle).toFloat()
                val startY = center.y - startRadius * cos(angle).toFloat()
                val endX = center.x + endRadius * sin(angle).toFloat()
                val endY = center.y - endRadius * cos(angle).toFloat()

                drawLine(
                    color = GlassBorder,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (degree % 90 == 0) 3f else 1.5f
                )
            }

            // Calculate satellite position
            // Elevation: 0° = center, 90° = edge
            val elevationRadius = radius * (1f - animatedElevation / 90f)
            val azimuthRad = Math.toRadians(animatedAzimuth.toDouble())

            val satelliteX = center.x + elevationRadius * sin(azimuthRad).toFloat()
            val satelliteY = center.y - elevationRadius * cos(azimuthRad).toFloat()
            val satellitePos = Offset(satelliteX, satelliteY)

            // Draw line from center to satellite
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CosmicBlue.copy(alpha = 0.5f),
                        CosmicBlue.copy(alpha = 0.1f)
                    ),
                    start = center,
                    end = satellitePos
                ),
                start = center,
                end = satellitePos,
                strokeWidth = 3f
            )

            // Draw satellite indicator with glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NebulaPink.copy(alpha = 0.6f),
                        NebulaPink.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = satellitePos,
                    radius = 30f * pulseScale
                ),
                center = satellitePos,
                radius = 30f * pulseScale
            )

            // Draw satellite dot
            drawCircle(
                color = NebulaPink,
                center = satellitePos,
                radius = 12f
            )

            // Draw center dot
            drawCircle(
                color = AuroraGreen,
                center = center,
                radius = 8f
            )
            drawCircle(
                color = AuroraGreen.copy(alpha = 0.3f),
                center = center,
                radius = 16f
            )
        }

        // Overlay text information
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = satelliteName,
                style = MaterialTheme.typography.titleMedium,
                color = StarWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AZIMUTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.1f", animatedAzimuth)}°",
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ELEVATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.1f", animatedElevation)}°",
                        style = MaterialTheme.typography.titleLarge,
                        color = NebulaPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
