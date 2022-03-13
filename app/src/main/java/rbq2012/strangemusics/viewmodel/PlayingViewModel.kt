package rbq2012.strangemusics.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import rbq2012.strangemusics.model.Track
import java.lang.ref.WeakReference

class PlayingViewModel : ViewModel() {

    val track: MutableState<Track?> = mutableStateOf(null)

    val paused = mutableStateOf(true)

    var getProgress: (() -> Pair<Long, Long>?)? = null

    companion object {
        val default = PlayingViewModel()
    }

}