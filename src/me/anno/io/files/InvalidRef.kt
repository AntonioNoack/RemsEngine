package me.anno.io.files

import me.anno.io.VoidOutputStream
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

object InvalidRef : FileReference("") {

    private val LOGGER = LogManager.getLogger(InvalidRef::class)

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.err(FileNotFoundException("InvalidRef is no valid source"))
    }

    override fun outputStream(append: Boolean): OutputStream {
        LOGGER.warn("Cannot write to InvalidRef")
        return VoidOutputStream
    }

    override fun length() = 0L

    override fun delete(): Boolean {
        LOGGER.warn("Cannot delete InvalidRef")
        return false
    }

    override fun mkdirs(): Boolean {
        LOGGER.warn("Cannot make InvalidRef to folders")
        return false
    }

    override fun listChildren(callback: Callback<List<FileReference>>) {
        callback.ok(emptyList())
    }

    override fun getParent(): FileReference = InvalidRef
    override fun renameTo(newName: FileReference): Boolean {
        LOGGER.warn("Cannot rename InvalidRef")
        return false
    }

    override fun getChild(name: String): FileReference = InvalidRef
    override fun getChildImpl(name: String): FileReference = InvalidRef

    override val exists: Boolean get() = false
    override val isDirectory: Boolean get() = false

    override val lastModified: Long get() = 0L
    override val lastAccessed: Long get() = 0L
    override val creationTime: Long get() = 0L

    override fun toString() = ""
}