package me.anno.io.files

import me.anno.io.files.Reference.appendPath
import me.anno.io.files.Reference.getRealReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Proxy file, which can be created instantly.
 * */
class LinkFileReference(absolutePath: String) : FileReference(absolutePath) {

    private val original: FileReference
        get() {
            val file = getRealReference(absolutePath)
            assertTrue(file !is LinkFileReference) {
                "$absolutePath resolved to LinkFileReference"
            }
            assertEquals(absolutePath, file.absolutePath)
            return file
        }

    init {
        assertTrue(absolutePath.isNotEmpty())
    }

    override fun getChildImpl(name: String): FileReference {
        return getReference(appendPath(absolutePath, name))
    }

    override fun length(): Long = original.length()
    override fun delete(): Boolean = original.delete()
    override fun mkdirs(): Boolean = original.mkdirs()

    override fun getParent(): FileReference {
        var index = absolutePath.lastIndexOf('/')
        val parent = if (index < 0) {
            if (absolutePath == FileRootRef.name) InvalidRef
            else FileRootRef
        } else {
            assertTrue(index != absolutePath.lastIndex)
            if (index >= 2 && absolutePath[index - 1] == '/' && absolutePath[index - 2] == ':') index++
            getReference(absolutePath.substring(0, index))
        }
        return parent
    }

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