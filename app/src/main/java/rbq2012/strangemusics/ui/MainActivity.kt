package rbq2012.strangemusics.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rbq2012.strangemusics.R
import rbq2012.strangemusics.data.TracksStorage
import rbq2012.strangemusics.model.Folder
import rbq2012.strangemusics.preview.PreviewFoldersProvider
import rbq2012.strangemusics.ui.PlayingViews.NowPlayingIcon
import rbq2012.strangemusics.ui.theme.StrangeMusicsTheme
import rbq2012.strangemusics.viewmodel.ImportPackViewModel
import rbq2012.strangemusics.viewmodel.PlayingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrangeMusicsTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Main()
                }
            }
        }
    }

    companion object {

        @Composable
        fun Main(
            importPackViewModel: ImportPackViewModel = viewModel()
        ) {
            Scaffold(
                topBar = { ActionBar(importPackViewModel) },
                bottomBar = { PlayingViews.PlayingBar(PlayingViewModel.default) }
            ) {
                if (importPackViewModel.isInProgress.value)
                    LinearProgressIndicator(Modifier.fillMaxWidth(1f))
                FoldersList(TracksStorage.getFolders(LocalContext.current))
            }
            if (importPackViewModel.isAwaitingOverwriteConfirmation.value)
                PackImportViews.OverwriteConfirmationDialog(importPackViewModel)
        }

        @Composable
        private fun ActionBar(importPackViewModel: ImportPackViewModel = viewModel()) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val onPickedPackToImport =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    val data = uri?.let {
                        context.contentResolver.openInputStream(uri)
                    } ?: return@rememberLauncherForActivityResult
                    // we are not using this functionality more than 2 times a year,
                    // and the process won't take long unless with dying flash,
                    // so keep it in the lifecycle of an Activity won't be a problem
                    scope.launch {
                        importPackViewModel.isInProgress.value = true
                        withContext(Dispatchers.IO) {
                            TracksStorage.importPack(data, context) {
                                importPackViewModel.overwriteTarget.value = it
                                importPackViewModel.isAwaitingOverwriteConfirmation.value = true
                                importPackViewModel.overwriteConfirmationResult.take()
                            }
                        }
                        importPackViewModel.isInProgress.value = false
                    }
                }
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onPickedPackToImport.launch("application/zip") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Localized description")
                    }
                }
            )
        }


        @Composable
        private fun FoldersList(
            folders: List<Folder>
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(folders) { data ->
                    FolderItem(folder = data)
                }
            }
        }

        @Composable
        private fun FolderItem(folder: Folder) {
            val context = LocalContext.current
            Row(
                Modifier
                    .clickable {
                        context.startActivity(Intent(
                            context, FolderActivity::class.java
                        ).also {
                            it.putExtra("folder", folder)
                        })
                    }
                    .fillMaxWidth(1f)
                    .padding(PaddingValues(horizontal = 24.dp, vertical = 12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (PlayingViewModel.default.track.value?.path?.parentFile?.name == folder.name)
                    NowPlayingIcon()
                Text(text = folder.name)
            }
        }

        @Preview(showBackground = true)
        @Composable
        private fun TopBarPreview(
        ) {
            StrangeMusicsTheme {
                ActionBar()
            }
        }

        @Preview(showBackground = true)
        @Composable
        private fun FoldersListPreview(
            @PreviewParameter(PreviewFoldersProvider::class) folders: List<Folder>
        ) {
            StrangeMusicsTheme {
                FoldersList(folders)
            }
        }
    }
}