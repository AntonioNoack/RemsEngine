package me.anno.io.files

import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.FileNotFoundException
import java.io.InputStream

class ZipFile7z(
    absolutePath: String,
    val getZipStream: () -> SevenZFile,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        val entry = zis.entries.firstOrNull { it.name == relativePath }
        if (entry != null) {
            // target found
            return zis.getInputStream(entry)
        }
        throw FileNotFoundException(relativePath)
    }

}