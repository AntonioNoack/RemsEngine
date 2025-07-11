package me.anno.io.files

import me.anno.io.files.Reference.appendPath
import me.anno.io.files.Reference.getRealReference
import me.anno.io.files.Reference.getRealReferenceOrNull
import me.anno.io.files.Reference.getReference
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Proxy file, which can be created instantly.
 * */
class LinkFileReference(absolutePath: String) : FileReference(absolutePath) {

    companion object {
        private val LOGGER = LogManager.getLogger(LinkFileReference::class)
    }

    val original: FileReference
        get() {
            val file = getRealReference(absolutePath)
            if (file is LinkFileReference) {
                LOGGER.warn("Failed to resolve '$absolutePath', somehow got a LinkFileReference again")
                return InvalidRef
            }
            if (absolutePath != file.absolutePath) {
                LOGGER.warn("Failed to resolve '$absolutePath', got '${file.absolutePath}'")
                return InvalidRef
            }
            return file
        }

    val originalOrNull: FileReference?
        get() = getRealReferenceOrNull(absolutePath)

    override fun getChildImpl(name: String): FileReference {
        return getReference(appendPath(absolutePath, name))
    }

    override fun length(): Long = original.length()
    override fun delete(): Boolean = original.delete()
    override fun mkdirs(): Boolean = original.mkdirs()

    override fun listChildren(callback: Callback<List<FileReference>>) = original.listChildren(callback)

    override fun renameTo(newName: FileReference): Boolean = original.renameTo(newName)

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) =
        original.inputStream(lengthLimit, closeStream, callback)

    override fun outputStream(append: Boolean): OutputStream =
        original.outputStream(append)

    override fun readBytes(callback: Callback<ByteArray>) =
        original.readBytes(callback)

    override fun readByteBuffer(native: Boolean, callback: Callback<ByteBuffer>) =
        original.readByteBuffer(native, callback)

    override val isDirectory: Boolean
        get() = original.isDirectory
    override val exists: Boolean
        get() = original.exists
    override val lastModified: Long
        get() = original.lastModified
    override val lastAccessed: Long
        get() = original.lastAccessed
    override val creationTime: Long
        get() = original.creationTime
}