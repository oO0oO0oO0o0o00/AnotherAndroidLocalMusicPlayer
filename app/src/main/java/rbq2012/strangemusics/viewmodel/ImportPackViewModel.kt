package rbq2012.strangemusics.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.concurrent.LinkedBlockingDeque

class ImportPackViewModel: ViewModel() {

    val isAwaitingOverwriteConfirmation = mutableStateOf(false)

    val overwriteTarget = mutableStateOf("")

    val overwriteConfirmationResult = LinkedBlockingDeque<Boolean>(1)

    val isInProgress = mutableStateOf(false)
}