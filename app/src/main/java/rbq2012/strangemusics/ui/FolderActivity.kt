package rbq2012.strangemusics.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import rbq2012.strangemusics.data.TracksStorage
import rbq2012.strangemusics.model.Folder
import rbq2012.strangemusics.model.Track
import rbq2012.strangemusics.service.MusicService
import rbq2012.strangemusics.ui.ui.theme.StrangeMusicsTheme
import rbq2012.strangemusics.viewmodel.PlayingViewModel

class FolderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val folder = intent.getSerializableExtra("folder") as Folder
        TracksStorage.loadFolder(folder, this)
        setContent {
            StrangeMusicsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Main(folder, onBack = { finish() })
                }
            }
        }
    }

    companion object {

        @Composable
        fun Main(folder: Folder, onBack: () -> Unit) {
            Scaffold(topBar = { ActionBar(folder, onBack) },
                bottomBar = { PlayingViews.PlayingBar(PlayingViewModel.default) }) {
                TracksList(folder)
            }
        }

        @SuppressLint("UnsafeOptInUsageError")
        @Composable
        private fun TracksList(
            folder: Folder,
            modifier: Modifier = Modifier
        ) {
            val context = LocalContext.current
            val playingThisFolder =
                PlayingViewModel.default.track.value?.path?.parentFile?.name == folder.name
            LazyColumn(
                modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(folder.contents) { data ->
                    TrackItem(track = data, playingThisFolder = playingThisFolder) {
                        MusicService.play(
                            context, tracks = folder.contents.toTypedArray(),
                            startsFrom = folder.contents.indexOf(it)
                        )
                        PlayingActivity.start(context)
                    }
                }
            }
        }

        @Composable
        private fun TrackItem(track: Track, playingThisFolder: Boolean, onPlay: (Track) -> Unit) {
            Row(Modifier
                .clickable {
                    onPlay(track)
                }
                .fillMaxWidth(1f)
                .padding(PaddingValues(horizontal = 24.dp, vertical = 16.dp)),
                verticalAlignment = Alignment.CenterVertically) {
                if (playingThisFolder && PlayingViewModel.default.track.value?.path?.name == track.filename)
                    PlayingViews.NowPlayingIcon()
                Text(text = track.filename)
            }
        }

        @Composable
        private fun ActionBar(folder: Folder, onBack: () -> Unit) {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "back")
                    }
                },
            )
        }
    }

}
