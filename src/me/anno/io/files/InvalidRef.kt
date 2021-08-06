package me.anno.io.files

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

object InvalidRef : FileReference("") {

    override fun inputStream(): InputStream {
        throw FileNotFoundException()
    }

    override fun outputStream(): OutputStream {
        throw FileNotFoundException()
    }

    override fun length(): Long = 0

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

    override fun listChildren(): List<FileReference>? = null

    override fun getParent(): FileReference? = null
    override fun renameTo(newName: FileReference): Boolean {
        return false
    }

    // a local file...
    override fun getChild(name: String): FileReference = InvalidRef

    override val isDirectory: Boolean
        get() = false

    override val exists: Boolean
        get() = false

    override val lastModified: Long
        get() = 0L

    override val lastAccessed: Long
        get() = 0L

    override fun toUri(): URI {
        return URI("file:/invalid")
    }

    override fun toString(): String {
        return "InvalidRef"
    }

}