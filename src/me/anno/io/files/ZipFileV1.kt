package me.anno.io.files

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.zip.ZipInputStream

class ZipFileV1(
    absolutePath: String,
    val getZipStream: () -> ZipInputStream,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        while (true) {
            val entry = zis.nextEntry ?: break
            if (entry.name == relativePath) {
                // target found
                return zis
            }
        }
        throw FileNotFoundException(relativePath)
    }

}