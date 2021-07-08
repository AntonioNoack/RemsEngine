package me.anno.io.files

import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.InputStream
import java.io.OutputStream

class ZipFileV2(
    absolutePath: String,
    val getZipStream: () -> ZipFile,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun length(): Long = size

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        return zis.getInputStream(zis.getEntry(relativePath))
    }

    override fun outputStream(): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }
}