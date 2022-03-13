package rbq2012.strangemusics.ui

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import rbq2012.strangemusics.viewmodel.ImportPackViewModel

object PackImportViews {

    @Composable
    fun OverwriteConfirmationDialog(
        viewModel: ImportPackViewModel
    ) {
        AlertDialog(onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.overwriteConfirmationResult.put(true)
                    viewModel.isAwaitingOverwriteConfirmation.value = false
                }) {
                    Text(text = "Overwrite")
                }
            }, dismissButton = {
                TextButton(onClick = {
                    viewModel.overwriteConfirmationResult.put(false)
                    viewModel.isAwaitingOverwriteConfirmation.value = false
                }) {
                    Text(text = "Skip")
                }
            }, title = { Text("Overwrite...") },
            text = { Text("Folder \"${viewModel.overwriteTarget.value}\" already exist") }
        )
    }

}