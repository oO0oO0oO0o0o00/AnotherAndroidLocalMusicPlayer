package rbq2012.strangemusics.ui

import androidx.compose.animation.core.*
import androidx.compose.runtime.*

// https://nascimpact.medium.com/jetpack-compose-working-with-rotation-animation-aeddc5899b28

@Composable
fun rememberRotationAnimation(
    rotating: Boolean, millisPerCycle: Int = 3000, slowDownMillis: Int = 1000
): Float {
    var currentRotation by remember { mutableStateOf(0f) }

    val rotation = remember { Animatable(currentRotation) }

    LaunchedEffect(rotating) {
        if (rotating) {
            // Infinite repeatable rotation when is playing
            rotation.animateTo(
                targetValue = currentRotation + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(millisPerCycle, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                currentRotation = value
            }
        } else {
            if (currentRotation > 0f) {
                // Slow down rotation on pause
                rotation.animateTo(
                    targetValue = currentRotation + (
                            slowDownMillis.toFloat() / millisPerCycle / 360).toInt(),
                    animationSpec = tween(
                        durationMillis = slowDownMillis,
                        easing = LinearOutSlowInEasing
                    )
                ) {
                    currentRotation = value
                }
            }
        }
    }
    return rotation.value
}
