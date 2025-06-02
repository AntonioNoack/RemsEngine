package me.anno.io.links

import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerLinkFile
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.waitForCallback
import me.anno.utils.files.LocalFile.toGlobalFile
import java.io.IOException

object URLReader {

    suspend fun readURLAsFolder(src: FileReference): Result<InnerFolder> {
        return waitForCallback { readURLAsFolder(src, it) }
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun readURLAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
        file.readLines(1024) { lines, exception ->
            if (lines == null) callback.err(exception)
            else {
                val files = ArrayList<FileReference>()
                for (line in lines) {
                    val fileI = if (line.startsWith("URL=file://")) {
                        line.substring(11).toGlobalFile()
                    } else if (line.startsWith("URL=")) {
                        getReference(line.substring(4))
                    } else continue
                    files.add(fileI)
                }
                lines.close()
                if (files.isNotEmpty()) {
                    val folder = InnerFolder(file)
                    var j = 0
                    for (i in files.indices) {
                        val child = files[i]
                        var key = child.name
                        // ensure unique name
                        if (key in folder.children) {
                            var ext = file.extension
                            if (ext.isNotEmpty()) ext = ".$ext"
                            key = "${j++}$ext"
                            while (key in folder.children) {
                                key = "${j++}$ext"
                            }
                        }
                        // create child & add it
                        InnerLinkFile(folder, key, child)
                    }
                    callback.ok(folder)
                } else callback.err(IOException("No files were found in $file"))
            }
        }
    }
}