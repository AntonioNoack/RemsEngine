package me.anno.io.files

import kotlinx.coroutines.runBlocking
import me.anno.Engine
import me.anno.cache.AsyncCacheData
import me.anno.cache.ICacheData
import me.anno.engine.EngineBase
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.Reference.appendPath
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.maths.Maths.min
import me.anno.utils.OSFeatures
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.mapSuccess2
import me.anno.utils.async.waitForCallback
import me.anno.utils.files.LocalFile.toLocalPath
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.arrays.ByteArrayList
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.indexOf2
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

        fun isValidName(name: String): Boolean {
            return '/' in name || '\\' in name || name == ".." || name == "."
        }
    }

    private var isValid = true

    val name: String
    val nameWithoutExtension: String
    val extension: String
    val lcExtension: String // the extension is often required in lowercase, so we cache it here

    init {
        assertTrue('\\' !in absolutePath, "Path must not contain backwards slashes")
        assertFalse(
            absolutePath.endsWith("/") && !absolutePath.endsWith("://"),
            "Path must not end with slash, except for protocol roots"
        )
    }

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

    fun resolved(): FileReference {
        return if (this is LinkFileReference) original
        else this
    }

    private val _hashCode = absolutePath.hashCode()

    var isHidden = name.startsWith('.')// hidden file in Linux, or file in unity package

    fun hide() {
        isHidden = true
    }

    open fun getChild(name: String): FileReference {
        return getReference(appendPath(absolutePath, name))
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun getChildUnsafe(name: String): FileReference {
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
            result = getChildImplI(result, nameJ)
            if (result == InvalidRef) {
                return InvalidRef
            }
            i = ni + 1
        }
        if (i < nameI.length) {
            result = getChildImplI(result, nameI.substring(i))
        }
        return result
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    private fun getChildImplI(result: FileReference, nameJ: String): FileReference {
        return when (nameJ) {
            "." -> result
            ".." -> result.getParent()
            else -> result.getChildImpl(nameJ)
        }
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract fun getChildImpl(name: String): FileReference

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract fun length(): Long

    abstract fun delete(): Boolean

    abstract fun mkdirs(): Boolean

    abstract fun getParent(): FileReference

    abstract fun renameTo(newName: FileReference): Boolean

    fun getChildImplOrNull(name: String): FileReference? =
        getChildImpl(name).nullIfUndefined()

    fun getNameWithExtension(ext: String): String {
        return "$nameWithoutExtension.$ext"
    }

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
    @Deprecated(USE_COROUTINES_INSTEAD)
    abstract fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>)

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) = inputStream(lengthLimit, true, callback)

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun inputStream(callback: Callback<InputStream>) = inputStream(Long.MAX_VALUE, true, callback)

    // todo make this the standard implementation, not inputStream with callback
    suspend fun inputStream(lengthLimit: Long = Long.MAX_VALUE): Result<InputStream> {
        return waitForCallback { inputStream(lengthLimit, false, it) }
    }

    /**
     * give access to an output stream;
     * should be buffered for better performance
     * */
    abstract fun outputStream(append: Boolean = false): OutputStream

    open fun readText(callback: Callback<String>) {
        readBytes(callback.map { it.decodeToString() })
    }

    open fun readBytes(callback: Callback<ByteArray>) {
        inputStream(callback.map { it.readBytes() })
    }

    suspend fun readBytes(): Result<ByteArray> {
        return waitForCallback(::readBytes)
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun inputStreamSync(): InputStream = readSync { inputStream(Long.MAX_VALUE, false, it) }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun readBytesSync(): ByteArray = readSync(::readBytes)

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun readTextSync(): String = readSync(::readText)

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun readByteBufferSync(native: Boolean): ByteBuffer = readSync { readByteBuffer(native, it) }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
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

    @Deprecated(USE_COROUTINES_INSTEAD)
    open fun readByteBuffer(native: Boolean, callback: Callback<ByteBuffer>) {
        readBytes(callback.map { bytes ->
            if (native) {
                val buffer = ByteBufferPool.allocateDirect(bytes.size)
                buffer.put(bytes).flip()
                buffer
            } else ByteBuffer.wrap(bytes)
        })
    }

    suspend fun readByteBuffer(native: Boolean): Result<ByteBuffer> {
        return readBytes().mapSuccess2 { bytes ->
            if (native) {
                val buffer = ByteBufferPool.allocateDirect(bytes.size)
                buffer.put(bytes).flip()
                buffer
            } else ByteBuffer.wrap(bytes)
        }
    }

    open fun readLines(lineLengthLimit: Int, callback: Callback<ReadLineIterator>) {
        readLinesImpl(lineLengthLimit, true, callback)
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun readLinesSync(lineLengthLimit: Int): ReadLineIterator {
        return readSync { readLinesImpl(lineLengthLimit, false, it) }
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    private fun readLinesImpl(lineLengthLimit: Int, closeStream: Boolean, callback: Callback<ReadLineIterator>) {
        inputStream(Long.MAX_VALUE, closeStream, callback.map { stream ->
            ReadLineIterator(stream.bufferedReader(), lineLengthLimit)
        })
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
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

    @Deprecated(USE_COROUTINES_INSTEAD)
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

    fun writeBytes(bytes: ByteArrayList) {
        writeBytes(bytes.values, 0, bytes.size)
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
        val ignoreCase = !OSFeatures.filesAreCaseSensitive
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

    fun tryMkdirs(): Boolean {
        return try {
            mkdirs()
        } catch (e: Exception) {
            LOGGER.warn("Failed to create ${toString()}", e)
            false
        }
    }

    open fun deleteOnExit() {
        Engine.registerForShutdown {
            delete()
        }
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    val zipFileForDirectory: FileReference?
        get() = AsyncCacheData.loadSync(this::zipFileForDirectory)

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun zipFileForDirectory(callback: Callback<FileReference>) {
        val file = this
        AsyncCacheData.runOnNonGFXThread("zipFor($file)") {
            runBlocking {
                var zipFile = InnerFolderCache.readAsFolderX(file).await().getOrNull()
                if (zipFile != null && zipFile !== file && !zipFile.isDirectory) {
                    zipFile = InnerFolderCache.readAsFolderX(zipFile).await().getOrNull() ?: zipFile
                }
                callback.call(zipFile, null)
            }
        }
    }

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

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    open fun isSerializedFolder(): Boolean {
        // only read the first bytes
        val signature = SignatureCache[this, false]
        return InnerFolderCache.getReaders(signature, lcExtension).isNotEmpty()
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract val isDirectory: Boolean

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract val exists: Boolean

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract val lastModified: Long

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract val lastAccessed: Long

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    abstract val creationTime: Long

    suspend fun isDirectory(): Boolean {
        return isDirectory
    }

    suspend fun exists(): Boolean {
        return exists
    }

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

    open fun listChildren(callback: Callback<List<FileReference>>) {
        InnerFolderCache.readAsFolder(this) { folder, _ ->
            if (folder is InnerFolder) folder.listChildren(callback)
            else callback.ok(emptyList())
        }
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun listChildren(): List<FileReference> {
        return AsyncCacheData.loadSync(this::listChildren) ?: emptyList()
    }

    suspend fun listChildrenX(): Result<List<FileReference>> {
        return waitForCallback { callback -> listChildren(callback) }
    }

    fun nullIfUndefined(): FileReference? {
        return if (this == InvalidRef) null
        else this
    }

    fun ifUndefined(other: FileReference): FileReference {
        return if (this == InvalidRef) other
        else this
    }

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun printTree(depth: Int = 0) {
        LOGGER.info("${Strings.spaces(depth * 2)}$name")
        if (isDirectory) {
            for (child in listChildren()) {
                child.printTree(depth + 1)
            }
        }
    }

    fun isSameOrSubFolderOf(maybeParent: FileReference): Boolean {
        return isSameOrSubFolderOf(maybeParent.absolutePath)
    }

    fun isSameOrSubFolderOf(maybeParent: String): Boolean {
        return absolutePath == maybeParent || isSubFolderOf(maybeParent)
    }

    fun isSubFolderOf(other: FileReference): Boolean {
        return isSubFolderOf(other.absolutePath)
    }

    fun isSubFolderOf(other: String): Boolean {
        val path = absolutePath
        return path.length > other.length + 1 &&
                path[other.length] == '/' &&
                path.startsWith(other)
    }

    /**
     * Replaces the sub-path of oldName with newName, if possible.
     * If not, returns null.
     * */
    fun replacePath(oldName: String, newName: FileReference): FileReference? {
        if (absolutePath == oldName) return newName
        if (!isSubFolderOf(oldName)) return null
        assertEquals('/', absolutePath[oldName.length])
        val commonSubFile = absolutePath.substring(oldName.length + 1)
        return newName.getChild(commonSubFile)
    }

    /**
     * Replaces the sub-path of oldName with newName, if possible.
     * If not, returns null.
     * */
    fun replacePath(oldName: FileReference, newName: FileReference): FileReference? {
        if (oldName == InvalidRef) return null
        return replacePath(oldName.absolutePath, newName)
    }
}