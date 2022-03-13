package rbq2012.strangemusics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import rbq2012.strangemusics.R
import rbq2012.strangemusics.service.MusicService
import rbq2012.strangemusics.ui.ui.theme.StrangeMusicsTheme
import rbq2012.strangemusics.viewmodel.PlayingViewModel
import kotlin.math.max


class PlayingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrangeMusicsTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Main(PlayingViewModel.default, onBack = { finish() })
                }
            }
        }
    }

    companion object {

        @SuppressLint("UnsafeOptInUsageError")
        @Composable
        fun Main(playingViewModel: PlayingViewModel = viewModel(), onBack: () -> Unit) {
            val progressState = remember { PlayingProgressState() }
            val context = LocalContext.current
            LaunchedEffect(playingViewModel.paused.value) {
                do {
                    playingViewModel.getProgress?.invoke()?.run {
                        if (!progressState.seeking) progressState.progress = first
                        progressState.trackDuration = max(second, 0L)
                    }
                    delay(500L)
                } while (!playingViewModel.paused.value)
            }
            Scaffold(topBar = { ActionBar(playingViewModel, onBack) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Row(
                        Modifier
                            .height(320.dp)
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                            .background(MaterialTheme.colors.primary)
                    ) {}
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                DateUtils.formatElapsedTime(progressState.progress / 1000),
                                Modifier.align(Alignment.CenterStart)
                            )
                            Text(
                                DateUtils.formatElapsedTime(progressState.trackDuration / 1000),
                                Modifier.align(Alignment.CenterEnd)
                            )
                        }
                        PlayerSeekBar(progressState) {
                            MusicService.seek(context, at = progressState.progress)
                            progressState.seeking = false
                        }
                        PlayerControllButtons(playingViewModel)
                    }
                }
            }
        }

        @Composable
        private fun PlayerSeekBar(
            state: PlayingProgressState = PlayingProgressState(),
            onSeek: (() -> Unit)
        ) {
            Slider(
                state.progress.toFloat(),
                onValueChange = { state.seeking = true; state.progress = it.toLong() },
                onValueChangeFinished = onSeek,
                valueRange = 0f..state.trackDuration.toFloat()
            )
        }

        @SuppressLint("UnsafeOptInUsageError")
        @Composable
        private fun PlayerControllButtons(
            playingViewModel: PlayingViewModel = viewModel()
        ) {
            val context = LocalContext.current
            val sizeModifier = Modifier.size(64.dp)
            CompositionLocalProvider(LocalIndication provides rememberRipple(bounded = false)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_skip_previous_24),
                        "go previous",
                        sizeModifier then Modifier.clickable {
                            MusicService.previous(context)
                        })
                    PlayingViews.PlayPauseButton(
                        playingViewModel.paused.value,
                        sizeModifier then Modifier.padding(8.dp)
                    ) { paused -> MusicService.pauseOrResume(context, !paused) }
                    Icon(
                        painterResource(R.drawable.ic_baseline_skip_next_24),
                        "go next",
                        sizeModifier then Modifier.clickable {
                            MusicService.goNext(context)
                        })
                }
            }
        }

        @Composable
        private fun ActionBar(
            playingViewModel: PlayingViewModel = viewModel(),
            onBack: () -> Unit
        ) {
            TopAppBar(
                title = {
                    MarqueenText(
                        playingViewModel.track.value?.name ?: "Music",
                        modifier = Modifier.padding(end = 10.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "back")
                    }
                },
                elevation = 0.dp
            )
        }

        fun start(context: Context){
            context.startActivity(Intent(context, PlayingActivity::class.java))
        }

        @Stable
        class PlayingProgressState {
            var seeking by mutableStateOf(false)
            var progress by mutableStateOf(0L)
            var trackDuration by mutableStateOf(0L)
        }

        @Preview(showBackground = true)
        @Composable
        private fun PlayPauseButtonPreview(
        ) {
            StrangeMusicsTheme {
                val paused by remember { mutableStateOf(false) }
                PlayingViews.PlayPauseButton(paused) {}
            }
        }
    }

}
