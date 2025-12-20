package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

@Composable
fun AnimatedMetricCard(
    label: String,
    value: String,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pulsing animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "metric_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .background(
                brush = if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            NebulaPurple.copy(alpha = pulseAlpha),
                            CosmicBlue.copy(alpha = pulseAlpha * 0.5f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isActive) StarWhite else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isActive) NebulaPink else CosmicBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
