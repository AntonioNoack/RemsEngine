package me.anno.io.files

import me.anno.utils.structures.Callback
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

object InvalidRef : FileReference("") {

    private val LOGGER = LogManager.getLogger(InvalidRef::class)

    override fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) {
        throw FileNotFoundException("InvalidRef is no valid source")
    }

    override fun outputStream(append: Boolean): OutputStream {
        throw FileNotFoundException("InvalidRef is no valid source")
    }

    override fun length() = 0L

    override fun deleteRecursively(): Boolean {
        LOGGER.warn("Cannot delete InvalidRef")
        return false
    }

    override fun deleteOnExit() {
        LOGGER.warn("Will not delete InvalidRef on exit, as it does not exist")
    }

    override fun delete(): Boolean {
        LOGGER.warn("Cannot delete InvalidRef")
        return false
    }

    override fun mkdirs(): Boolean {
        LOGGER.warn("Cannot make InvalidRef to folders")
        return false
    }

    override fun listChildren(): List<FileReference> = emptyList()

    override fun getParent(): FileReference = InvalidRef
    override fun renameTo(newName: FileReference): Boolean {
        LOGGER.warn("Cannot rename InvalidRef")
        return false
    }

    // a local file...
    override fun getChild(name: String): FileReference = InvalidRef

    override val exists: Boolean get() = false
    override val isDirectory: Boolean get() = false
    override val isSomeKindOfDirectory: Boolean get() = false

    override val lastModified: Long get() = 0L
    override val lastAccessed: Long get() = 0L
    override val creationTime: Long get() = 0L

    override fun toUri(): URI {
        return URI("file:/invalid")
    }

    override fun toString() = ""

    override fun nullIfUndefined(): FileReference? = null
    override fun ifUndefined(other: FileReference) = other
}