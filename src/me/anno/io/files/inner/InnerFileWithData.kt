package me.anno.io.files.inner

import me.anno.io.EmptyInputStream
import me.anno.io.VoidOutputStream
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * a file, which is inside another file,
 * e.g., inside a zip file, or inside a mesh
 * */
abstract class InnerFileWithData(
    absolutePath: String,
    relativePath: String,
    parent: FileReference
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

    var compressedSize = 0L
    var size = 65536L // we don't know in this class
    var data: ByteArray? = null
    var isEncrypted = false

    override fun length(): Long = size

    override fun invalidate() {
        super.invalidate()
        markAsModified()
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        val data = data
        when {
            size <= 0 -> callback.call(EmptyInputStream, null)
            data != null -> callback.call(ByteArrayInputStream(data), null)
            else -> callback.err(IOException("Missing data"))
        }
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        val data = data
        if (data != null) callback.ok(data)
        else super.readBytes(callback)
    }

    override fun outputStream(append: Boolean): OutputStream {
        LOGGER.warn("Writing into zip files is not yet supported, '$absolutePath'")
        return VoidOutputStream
    }

    companion object {
        private val LOGGER = LogManager.getLogger(InnerFileWithData::class)
    }
}