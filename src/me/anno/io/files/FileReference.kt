package me.anno.io.files

import me.anno.Engine
import me.anno.cache.ICacheData
import me.anno.engine.EngineBase
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.maths.Maths.min
import me.anno.utils.OS
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.async.Callback
import me.anno.utils.files.LocalFile.toLocalPath
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * doesn't call toLowerCase() for each comparison,
 * so it's hopefully a lot faster
 *
 * we don't modify files a lot, but we do use them for comparisons a lot
 * because of that, this "performance-wrapper" exists
 *
 * also this can be used to navigate to "pseudo"-files, like files inside zip containers,
 * files on the web, or local resources
 * */
abstract class FileReference(val absolutePath: String) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(FileReference::class)
    }

    // done if there is a !!, it's into a zip file -> it only needs to be a slash;
    // all zip files should be detected automatically
    // done if res:// at the start, then it's a local resource

    private var isValid = true

    val name: String
    val nameWithoutExtension: String
    val extension: String
    val lcExtension: String // the extension is often required in lowercase, so we cache it here

    init {
        val lastIndex = absolutePath.lastIndexOf('/')
        val endIndex = min(
            absolutePath.indexOf2('?', lastIndex + 1),
            absolutePath.indexOf2('&', lastIndex + 1)
        )
        name = absolutePath.substring(lastIndex + 1, endIndex)
        val extIndex = name.lastIndexOf('.')
        if (extIndex < 0) {
            extension = ""
            lcExtension = ""
            nameWithoutExtension = name
        } else {
            extension = name.substring(extIndex + 1)
            lcExtension = extension.lowercase()
            nameWithoutExtension = name.substring(0, extIndex)
        }
    }

    private val _hashCode = absolutePath.hashCode()

    private val _hasValidName = !absolutePath.isBlank2()
    fun hasValidName() = _hasValidName

    var isHidden = name.startsWith('.')// hidden file in Linux, or file in unity package

    fun hide() {
        isHidden = true
    }

    fun getChild(name: String): FileReference {
        return getChildUnsafe(name, true)
    }

    fun getChildUnsafe(name: String, onlyChildren: Boolean): FileReference {
        if (this == InvalidRef) return InvalidRef
        val nameI = if ('\\' in name) { // please, don't use back-slashes
            name.replace('\\', '/')
        } else name
        var i = 0
        var result = this
        while (true) {
            val ni = nameI.indexOf('/', i)
            if (ni < 0) break
            val nameJ = nameI.substring(i, ni)
            result = if (nameJ == "..") {
                if (onlyChildren) InvalidRef
                else result.getParent()
            } else result.getChildImpl(nameJ)
            if (result == InvalidRef) {
                return InvalidRef
            }
            i = ni + 1
        }
        if (i < nameI.length) {
            result = result.getChildImpl(nameI.substring(i))
        }
        return result
    }

    abstract fun getChildImpl(name: String): FileReference

    abstract fun length(): Long

    abstract fun delete(): Boolean

    abstract fun mkdirs(): Boolean

    abstract fun getParent(): FileReference

    abstract fun renameTo(newName: FileReference): Boolean

    fun getChildOrNull(name: String): FileReference? =
        getChild(name).nullIfUndefined()

    fun getNameWithExtension(ext: String): String {
        return "$nameWithoutExtension.$ext"
    }

    open fun hasChildren(): Boolean = listChildren().isNotEmpty()

    open fun invalidate() {
        LOGGER.info("Invalidated {}", absolutePath)
        isValid = false
        // if this has inner folders, replace all of their children as well
        InnerFolderCache.wasReadAsFolder(this)?.invalidate()
        Thumbs.invalidate(this)
        LastModifiedCache.invalidate(this)
    }

    fun validate(): FileReference {
        return if (isValid) this
        else getReference(absolutePath).ifUndefined(this)
    }

    /**
     * give access to an input stream;
     * should be buffered for better performance
     * */
    abstract fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>)
    fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) = inputStream(lengthLimit, true, callback)
    fun inputStream(callback: Callback<InputStream>) = inputStream(Long.MAX_VALUE, true, callback)

    /**
     * give access to an output stream;
     * should be buffered for better performance
     * */
    abstract fun outputStream(append: Boolean = false): OutputStream

    open fun readText(callback: Callback<String>) {
        readBytes { it, exc ->
            callback.call(it?.decodeToString(), exc)
        }
    }

    open fun readBytes(callback: Callback<ByteArray>) {
        inputStream { it, exc ->
            if (it != null) try {
                val bytes = it.readBytes()
                callback.call(bytes, null)
            } catch (e: Exception) {
                callback.call(null, e)
            } finally {
                it.close()
            } else callback.call(null, exc)
        }
    }

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    open fun inputStreamSync(): InputStream = readSync(::inputStream)

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    open fun readBytesSync(): ByteArray = readSync(::readBytes)

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    open fun readTextSync(): String = readSync(::readText)

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    open fun readByteBufferSync(native: Boolean): ByteBuffer = readSync { readByteBuffer(native, it) }

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    private fun <V> readSync(reader: (Callback<V>) -> Unit): V {
        var e: Exception? = null
        var d: V? = null
        reader { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    open fun readByteBuffer(native: Boolean, callback: Callback<ByteBuffer>) {
        readBytes { bytes, exc ->
            val buffer = if (bytes != null) {
                if (native) {
                    val buffer = ByteBufferPool.allocateDirect(bytes.size)
                    buffer.put(bytes).flip()
                    buffer
                } else ByteBuffer.wrap(bytes)
            } else null
            callback.call(buffer, exc)
        }
    }

    open fun readLines(lineLengthLimit: Int, callback: Callback<ReadLineIterator>) {
        readLinesImpl(lineLengthLimit, true, callback)
    }

    @Deprecated("Please use asynchronous methods where possible. Reading sync is a hassle/impossible on some platforms")
    open fun readLinesSync(lineLengthLimit: Int): ReadLineIterator {
        return readSync { readLinesImpl(lineLengthLimit, false, it) }
    }

    private fun readLinesImpl(lineLengthLimit: Int, closeStream: Boolean, callback: Callback<ReadLineIterator>) {
        inputStream(Long.MAX_VALUE, closeStream) { it, exc ->
            if (it != null) {
                val reader = it.bufferedReader()
                callback.call(ReadLineIterator(reader, lineLengthLimit), null)
            } else callback.call(null, exc)
        }
    }

    open fun writeFile(
        file: FileReference,
        progress: (delta: Long, total: Long) -> Unit,
        callback: (Exception?) -> Unit
    ) {
        file.inputStream { input, exc ->
            if (input != null) {
                outputStream().use { output ->
                    var total = 0L
                    val buffer = ByteArray(2048)
                    while (true) {
                        val numReadBytes = input.read(buffer)
                        if (numReadBytes < 0) break
                        output.write(buffer, 0, numReadBytes)
                        total += numReadBytes
                        progress(numReadBytes.toLong(), total)
                    }
                }
                callback(null)
            } else callback(exc)
        }
    }

    fun writeFile(file: FileReference, callback: (Exception?) -> Unit) {
        writeFile(file, { _, _ -> }, callback)
    }

    open fun writeText(text: String, offset: Int, length: Int) {
        val bytes = text.encodeToByteArray(offset, offset + length)
        writeBytes(bytes, 0, bytes.size)
    }

    fun writeText(text: String) {
        writeText(text, 0, text.length)
    }

    open fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        val os = outputStream()
        os.write(bytes, offset, length)
        os.close()
    }

    fun writeBytes(bytes: ByteArray) {
        writeBytes(bytes, 0, bytes.size)
    }

    open fun writeBytes(bytes: ByteBuffer) {
        val byte2 = ByteArray(bytes.remaining())
        val pos = bytes.position()
        bytes.get(byte2).position(pos)
        writeBytes(byte2, 0, byte2.size)
    }

    open fun relativePathTo(basePath: FileReference, maxNumBackPaths: Int): String? {
        val parts = absolutePath.split('/')
        val baseParts = basePath.absolutePath.split('/')
        var matchingStartPaths = 0 // those can be skipped
        val ignoreCase = OS.isLinux || OS.isAndroid
        for (i in 0 until min(parts.size, baseParts.size)) {
            if (!parts[i].equals(baseParts[i], ignoreCase)) break
            matchingStartPaths++
        }
        if (baseParts.size - matchingStartPaths > maxNumBackPaths) return null
        // calculate size for result
        var resultSize = (baseParts.size - matchingStartPaths) * 3
        for (i in matchingStartPaths until parts.size) {
            resultSize += parts[i].length + 1
        }
        val result = StringBuilder(resultSize)
        for (i in matchingStartPaths until baseParts.size) {
            result.append(if (result.isEmpty()) ".." else "/..")
        }
        for (i in matchingStartPaths until parts.size) {
            if (result.isNotEmpty()) result.append('/')
            result.append(parts[i])
        }
        return result.toString()
    }

    fun findRecursively(maxDepth: Int, find: (FileReference) -> Boolean): FileReference? {
        if (find(this)) return this
        if (maxDepth > 0 && isDirectory) {
            val children = listChildren()
            for (child in children) {
                val r = child.findRecursively(maxDepth - 1, find)
                if (r != null) return r
            }
        }
        return null
    }

    fun tryMkdirs(): Boolean {
        return try {
            mkdirs()
        } catch (e: Exception) {
            LOGGER.warn("Failed to create ${toString()}")
            false
        }
    }

    open fun deleteOnExit() {
        Engine.registerForShutdown {
            delete()
        }
    }

    val zipFileForDirectory: FileReference?
        get() {
            var zipFile = zipFile ?: return null
            if (!zipFile.isDirectory) {
                zipFile = InnerFolderCache.readAsFolder(zipFile, false) ?: return null
            }
            return zipFile
        }

    private val zipFile: FileReference?
        get() = InnerFolderCache.readAsFolder(this, false)

    fun getSibling(name: String): FileReference {
        return getParent().getChild(name)
    }

    fun getSiblingWithExtension(ext: String): FileReference {
        return getParent().getChild("$nameWithoutExtension.$ext")
    }

    fun copyTo(
        dst: FileReference,
        progressCallback: (delta: Long, total: Long) -> Unit,
        finishCallback: (Exception?) -> Unit
    ) {
        dst.writeFile(this, progressCallback, finishCallback)
    }

    fun copyTo(dst: FileReference, finishCallback: (Exception?) -> Unit) {
        copyTo(dst, { _, _ -> }, finishCallback)
    }

    fun copyTo(dst: FileReference) {
        copyTo(dst) {}
    }

    abstract val isDirectory: Boolean

    open fun isSerializedFolder(): Boolean {
        // only read the first bytes
        val signature = SignatureCache[this, false]
        return InnerFolderCache.getReaders(signature, lcExtension).isNotEmpty()
    }

    abstract val exists: Boolean
    abstract val lastModified: Long
    abstract val lastAccessed: Long
    abstract val creationTime: Long

    override fun equals(other: Any?): Boolean {
        return other is FileReference &&
                other._hashCode == _hashCode &&
                other.absolutePath == absolutePath
    }

    override fun hashCode(): Int {
        return _hashCode
    }

    override fun toString(): String {
        return absolutePath
    }

    open fun toLocalPath(workspace: FileReference = EngineBase.workspace): String {
        return absolutePath.toLocalPath(workspace)
    }

    open val isSomeKindOfDirectory get() = isDirectory || isPacked.value

    val isPacked = lazy {
        !isDirectory && isSerializedFolder()
    }

    open fun listChildren(): List<FileReference> {
        val folder = InnerFolderCache.readAsFolder(this, false)
        if (folder is InnerFolder) return folder.listChildren()
        if (folder != null) return listOf(folder)
        return emptyList()
    }

    fun nullIfUndefined(): FileReference? {
        return if (this == InvalidRef) null
        else this
    }

    fun ifUndefined(other: FileReference): FileReference {
        return if (this == InvalidRef) other
        else this
    }

    fun printTree(depth: Int = 0) {
        LOGGER.info("${Strings.spaces(depth * 2)}$name")
        if (isDirectory) {
            for (child in listChildren()) {
                child.printTree(depth + 1)
            }
        }
    }

    fun isSameOrSubFolderOf(other: FileReference): Boolean {
        if (other == InvalidRef) return false
        return this == other || isSubFolderOf(other.absolutePath)
    }

    fun isSameOrSubFolderOf(other: String): Boolean {
        return absolutePath == other || isSubFolderOf(other)
    }

    fun isSubFolderOf(other: FileReference): Boolean {
        if (other == InvalidRef) return false
        return isSubFolderOf(other.absolutePath)
    }

    fun isSubFolderOf(other: String): Boolean {
        val path = absolutePath
        return path.length > other.length + 1 &&
                path[other.length] == '/' &&
                path.startsWith(other)
    }
}