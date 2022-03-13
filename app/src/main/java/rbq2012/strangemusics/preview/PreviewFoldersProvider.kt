package rbq2012.strangemusics.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import rbq2012.strangemusics.model.Folder

class PreviewFoldersProvider : PreviewParameterProvider<List<Folder>> {
    override val values = sequenceOf(listOf(
        Folder("qwq", listOf()),
        Folder("TAT", listOf()),
        Folder("QAQ", listOf()),
    ))
}
