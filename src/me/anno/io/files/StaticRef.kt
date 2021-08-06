package me.anno.io.files

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

open class StaticRef(path: String, val content: Lazy<ByteArray>) : FileReference(path) {

    override fun getChild(name: String) = InvalidRef

    override fun inputStream(): InputStream = content.value.inputStream()

    override fun outputStream(): OutputStream {
        throw IOException("Permission denied on $absolutePath")
    }

    override fun length(): Long {
        return content.value.size.toLong()
    }

    override fun deleteRecursively(): Boolean {
        return false
    }

    override fun deleteOnExit() {

    }

    override fun delete(): Boolean {
        return false
    }

    override fun mkdirs(): Boolean {
        return false
    }

    override fun getParent(): FileReference? {
        return null
    }

    override fun renameTo(newName: FileReference): Boolean {
        return false
    }

    override val isDirectory: Boolean = false
    override val exists: Boolean = true
    override val lastModified: Long = 0L
    override val lastAccessed: Long = 0L

    override fun toUri(): URI {
        return URI("file:/$absolutePath")
    }


}