package rbq2012.strangemusics.data

import android.content.Context
import android.util.Log
import net.lingala.zip4j.io.inputstream.ZipInputStream
import rbq2012.strangemusics.model.Folder
import rbq2012.strangemusics.model.Track
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.RuntimeException

object TracksStorage {

    fun getFolders(context: Context): List<Folder> {
        val list = getStorageRoot(context).list() ?: throw RuntimeException("???")
        return list.map { Folder(it, listOf()) }
    }

    fun loadFolder(folder: Folder, context: Context) {
        folder.contents = (File(getStorageRoot(context), folder.name).listFiles() ?: return)
            .map { Track(it) }
    }

    fun findTrack(context: Context, path: String): Track {
        return Track(getStorageRoot(context).resolve(path))
    }

    private fun getStorageRoot(context: Context): File = File(context.filesDir, "tracks").apply {
        mkdirs()
    }

    fun importPack(
        inputStream: InputStream,
        context: Context,
        overwriteCallback: (String) -> Boolean
    ) {
        val storageRoot = getStorageRoot(context)
        val existing = storageRoot.listFiles() ?: throw RuntimeException("???")
        val zip = ZipInputStream(inputStream)
        val dstFolders = mutableMapOf<String, File>()
        while (true) {
            val entry = zip.nextEntry ?: break
            val srcPath = File(entry.fileName)
            val folderName = srcPath.name
            if (srcPath.parent == null) {
                // handle folders
                if (!entry.isDirectory || folderName[0] == '_') throw RuntimeException("???")
                var overwrite = false
                if (existing.any { it.name.equals(folderName) }) {
                    if (!overwriteCallback(folderName)) continue
                    overwrite = true
                }
                File(storageRoot, folderName).also {
                    if (overwrite) it.deleteRecursively()
                    dstFolders[folderName] = it
                }.mkdir()
            } else {
                // files
                if (entry.isDirectory) throw RuntimeException("???")
                val dstPath = srcPath.parent?.let { dstFolders[it] } ?: continue
                val os = FileOutputStream(File(dstPath, folderName))
                zip.copyTo(os)
            }
        }
    }
}