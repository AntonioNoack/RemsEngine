package me.anno.io.zip

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.EmptyInputStream
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * a file, which is inside another file,
 * e.g. inside a zip file, or inside a mesh
 * */
abstract class InnerFile(
    absolutePath: String,
    val relativePath: String,
    final override val isDirectory: Boolean,
    val _parent: FileReference
) : FileReference(absolutePath) {

    var folder: InnerFile? = null

    val lcName = name.lowercase()

    init {
        if (_parent is InnerFolder) {
            val old = _parent.children.put(name, this)
            _parent.childrenList.add(this)
            if (old != null) {
                _parent.childrenList.remove(old)
                old.folder = this
                _parent.children[name] = old
                // LOGGER.warn("Overrode $old")
            }
        }
    }

    // assigned typically anyway
    override var lastModified = 0L // _parent.lastModified
    override var lastAccessed = 0L // _parent.lastAccessed

    var compressedSize = 0L
    var size = 0L
    var data: ByteArray? = null
    var isEncrypted = false

    override fun length(): Long = size

    override fun inputStream(): InputStream {
        val data = data
        return when {
            size <= 0 -> EmptyInputStream
            data != null -> data.inputStream()
            else -> getInputStream().useBuffered()
        }
    }

    abstract fun getInputStream(): InputStream

    override fun readBytes(): ByteArray {
        return this.data ?: inputStream().readBytes()
    }

    override fun outputStream(): OutputStream {
        throw IOException("Writing into zip files is not yet supported, '$absolutePath'")
    }

    fun get(path: String) = getLc(path.replace('\\', '/').lowercase())

    open fun getLc(path: String): FileReference? {
        if (path.isEmpty()) return ZipCache.unzip(this, false)
        val m = ZipCache.unzip(this, false)
        return m?.getLc(path)
    }

    override fun getChild(name: String): FileReference {
        return ZipCache.unzip(this, false)?.getChild(name) ?: InvalidRef
    }

    override val exists: Boolean = true

    // override fun toString(): String = relativePath

    override fun toUri(): URI {
        return URI("zip://${absolutePath.replace(" ", "%20")}")
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
        return zipFileForDirectory?.listChildren()
    }

    override fun getParent(): FileReference {
        return _parent
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    companion object {

        // private val LOGGER = LogManager.getLogger(InnerFile::class)

        fun createMainFolder(zipFileLocation: FileReference): Pair<InnerFolder, HashMap<String, InnerFile>> {
            val file = InnerFolder(zipFileLocation)
            return file to createRegistry(file)
        }

        fun createRegistry(file: InnerFile): HashMap<String, InnerFile> {
            val registry = HashMap<String, InnerFile>()
            registry[""] = file
            return registry
        }

    }

}