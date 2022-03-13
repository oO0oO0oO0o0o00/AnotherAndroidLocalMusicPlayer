package rbq2012.strangemusics.model

import java.io.Serializable

data class Folder(var name: String, var contents: List<Track>): Serializable