package me.anno.io.files

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.zip.ZipInputStream

class ZipFile(
    absolutePath: String,
    val getZipStream: () -> ZipInputStream,
    val relativePath: String, override val isDirectory: Boolean, val parent: ZipFile?
) : FileReference(absolutePath) {

    val children = if (isDirectory) HashMap<String, ZipFile>() else null
    val lcName = name.lowercase()

    override var lastModified = 0L

    var lastAccessed = 0L
    var compressedSize = 0L
    var size = 0L
    var data: ByteArray? = null

    override fun length(): Long = size

    override fun inputStream(): InputStream {
        val data = data
        return if (size <= 0) {
            ByteArray(0).inputStream()
        } else if (data == null) {
            val zis = getZipStream()
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == relativePath) {
                    // target found
                    return zis
                }
            }
            throw FileNotFoundException(relativePath)
        } else {
            data.inputStream()
        }
    }

    override fun outputStream(): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    fun list() = children?.values?.map { it.name }
    fun listFiles() = children?.values

    fun get(path: String) = getLc(path.replace('\\', '/').lowercase())
    private fun getLc(path: String): ZipFile? {
        if (path.isEmpty() && !isDirectory)
            return ZipCache.getMeta(this, false)
        if (!isDirectory)
            return ZipCache.getMeta(this, false)?.getLc(path)
        val index = path.indexOf('/')
        return if (index < 0) {
            children!![path]
        } else {
            val parent = path.substring(0, index)
            val name = path.substring(index + 1)
            children!![parent]?.getLc(name)
        }
    }

    override fun getChild(name: String): FileReference? {
        if (children == null) return null
        val c0 = children.values.filter { it.name.equals(name, true) }
        return c0.firstOrNull { it.name == name } ?: c0.firstOrNull()
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

    override fun getParent(): FileReference? {
        return parent
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

}