package com.payfacto.braingames.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.payfacto.braingames.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val confettiColors = listOf(
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5),
    Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFFF6D00),
    Color(0xFF00ACC1), Color(0xFFEC407A)
)

private data class ConfettiParticle(
    val startX: Float,
    val startDelay: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val wobbleFreq: Float,
    val wobbleAmp: Float,
    val rotationSpeed: Float
)

@Composable
private fun ConfettiEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        List(60) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                startDelay = Random.nextFloat(),
                speed = 0.15f + Random.nextFloat() * 0.25f,
                width = 10f + Random.nextFloat() * 14f,
                height = 5f + Random.nextFloat() * 8f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                wobbleFreq = 1f + Random.nextFloat() * 2f,
                wobbleAmp = 0.03f + Random.nextFloat() * 0.05f,
                rotationSpeed = (if (Random.nextBoolean()) 1f else -1f) * (90f + Random.nextFloat() * 270f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "confetti_time"
    )

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val phase = (time + p.startDelay) % 1f
            val yFrac = phase * 1.2f - 0.1f
            val canvasY = yFrac * size.height
            val canvasX = p.startX * size.width +
                    sin((time * p.wobbleFreq * 2.0 * PI).toFloat()) * p.wobbleAmp * size.width
            val rotation = time * p.rotationSpeed

            withTransform({
                translate(left = canvasX, top = canvasY)
                rotate(degrees = rotation)
            }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.width / 2f, -p.height / 2f),
                    size = Size(p.width, p.height)
                )
            }
        }
    }
}

@Composable
fun WinAnimationOverlay(
    visible: Boolean,
    elapsedSeconds: Long,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            ConfettiEffect(modifier = Modifier.fillMaxSize())

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.win_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.win_time, formatElapsed(elapsedSeconds)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.win_play_again))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.win_home))
                    }
                }
            }
        }
    }
}

