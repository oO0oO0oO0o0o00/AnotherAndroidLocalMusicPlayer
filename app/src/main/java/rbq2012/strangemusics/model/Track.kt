package rbq2012.strangemusics.model

import java.io.File
import java.io.Serializable

data class Track(
    val path: File
) : Serializable {
    val filename: String
        get() = path.name
    val name: String
        get() = path.nameWithoutExtension
    val identifier: String
        get() = "${path.parentFile?.name?:""}/${path.name}"
}