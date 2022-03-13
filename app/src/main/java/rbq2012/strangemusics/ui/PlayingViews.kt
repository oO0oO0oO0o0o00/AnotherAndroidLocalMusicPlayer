package rbq2012.strangemusics.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import rbq2012.strangemusics.R
import rbq2012.strangemusics.service.MusicService
import rbq2012.strangemusics.viewmodel.PlayingViewModel

object PlayingViews {

    @SuppressLint("UnsafeOptInUsageError")
    @Composable
    fun PlayingBar(playingViewModel: PlayingViewModel = viewModel()) {
        val context = LocalContext.current
        LaunchedEffect(Unit) { MusicService.load(context) }

        if (playingViewModel.track.value != null) {
            val rotation = rememberRotationAnimation(
                !playingViewModel.paused.value,
                millisPerCycle = 5000, slowDownMillis = 800
            )
            Surface(elevation = AppBarDefaults.TopAppBarElevation,
                modifier = Modifier.clickable { PlayingActivity.start(context) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.ic_disc_48),
                        contentDescription = "spinning disc icon",
                        Modifier.rotate(rotation)
                    )
                    MarqueenText(
                        playingViewModel.track.value!!.name,
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                    )
                    CompositionLocalProvider(LocalIndication provides rememberRipple(bounded = false)) {
                        PlayPauseButton(
                            playingViewModel.paused.value,
                            Modifier.size(32.dp)
                        ) { paused -> MusicService.pauseOrResume(context, !paused) }
                    }
                }
            }
        }
    }

    @Composable
    fun PlayPauseButton(
        paused: Boolean,
        modifier: Modifier = Modifier,
        onPlayPause: (Boolean) -> Unit
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.playpause))
        val dynamicProperties = rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.COLOR,
                value = LocalContentColor.current.toArgb(),
                keyPath = arrayOf("**")
            ),
        )
        val progress = remember { Animatable(0f) }
        LaunchedEffect(paused) {
            progress.animateTo(
                if (paused) 0f else 1f, animationSpec = tween(
                    durationMillis = 250, delayMillis = 0,
                    easing = LinearEasing
                )
            )
        }
        LaunchedEffect(Unit) { progress.snapTo(if (paused) 0f else 1f) }
        LottieAnimation(
            composition, progress.value,
            dynamicProperties = dynamicProperties,
            modifier = Modifier.clickable { onPlayPause(paused) } then modifier
        )
    }

    @Composable
    fun NowPlayingIcon() {
        Icon(
            painterResource(id = R.drawable.ic_baseline_volume_up_24),
            "now playing", tint = MaterialTheme.colors.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
    }

}