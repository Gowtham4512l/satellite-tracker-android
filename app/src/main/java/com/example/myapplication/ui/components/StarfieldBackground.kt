package com.example.myapplication.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float
)

@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 150
) {
    val stars = remember {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2f + 0.5f,
                alpha = Random.nextFloat() * 0.5f + 0.3f,
                twinkleSpeed = Random.nextFloat() * 2f + 1f,
                twinkleOffset = Random.nextFloat() * 2f * Math.PI.toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starfield")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val twinkle = sin(time * star.twinkleSpeed + star.twinkleOffset) * 0.3f + 0.7f
            drawCircle(
                color = Color.White,
                radius = star.radius,
                center = Offset(
                    x = star.x * size.width,
                    y = star.y * size.height
                ),
                alpha = star.alpha * twinkle
            )
        }
    }
}
