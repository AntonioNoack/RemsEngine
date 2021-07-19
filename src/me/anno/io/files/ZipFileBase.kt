package me.anno.io.files

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.EmptyInputStream
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

abstract class ZipFileBase(
    absolutePath: String,
    val relativePath: String,
    override val isDirectory: Boolean,
    val _parent: FileReference
) : FileReference(absolutePath) {

    val children = if (isDirectory) HashMap<String, ZipFileBase>() else null
    val lcName = name.lowercase()

    init {
        (_parent as? ZipFileBase)?.children?.put(lcName, this)
    }

    override var lastModified = 0L
    override var lastAccessed = 0L

    var compressedSize = 0L
    var size = 0L
    var data: ByteArray? = null

    override fun length(): Long = size

    override fun inputStream(): InputStream {
        val data = data
        return if (size <= 0) {
            EmptyInputStream
        } else data?.inputStream() ?: return getInputStream().useBuffered()
    }

    abstract fun getInputStream(): InputStream

    override fun outputStream(): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    fun list() = children?.values?.map { it.name }
    fun listFiles() = children?.values

    fun get(path: String) = getLc(path.replace('\\', '/').lowercase())
    private fun getLc(path: String): FileReference? {
        if (path.isEmpty() && !isDirectory)
            return ZipCache.getMeta2(this, false)
        if (!isDirectory) {
            val m = ZipCache.getMeta2(this, false) as? ZipFileBase
            return m?.getLc(path)
        }
        val index = path.indexOf('/')
        return if (index < 0) {
            children!![path]
        } else {
            val parent = path.substring(0, index)
            val name = path.substring(index + 1)
            children!![parent]?.getLc(name)
        }
    }

    override fun getChild(name: String): FileReference {
        if (children == null) return InvalidRef
        val c0 = children.values.filter { it.name.equals(name, true) }
        return c0.firstOrNull { it.name == name } ?: c0.firstOrNull() ?: InvalidRef
    }

    override val exists: Boolean get() = true

    // override fun toString(): String = relativePath

    override fun toUri(): URI {
        return URI("zip://$absolutePath")
    }

    override fun deleteRecursively(): Boolean {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    override fun deleteOnExit() {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    override fun delete(): Boolean {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    override fun mkdirs(): Boolean {
        return false
    }

    override fun listChildren(): List<FileReference>? {
        return if (isDirectory) {
            children?.values?.toList()
        } else {
            zipFileForDirectory?.listChildren()
        }
    }

    override fun getParent(): FileReference {
        return _parent
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

}