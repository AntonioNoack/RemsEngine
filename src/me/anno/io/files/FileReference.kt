package me.anno.io.files

import me.anno.cache.ICacheData
import me.anno.engine.EngineBase
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Reference.getReferenceOrTimeout
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.files.thumbs.ThumbsExt
import me.anno.io.utils.WindowsShortcut
import me.anno.maths.Maths.min
import me.anno.utils.OS
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Tabs
import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.files.LocalFile.toLocalPath
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.strings.StringHelper.indexOf2
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.*
import java.net.URI
import java.nio.ByteBuffer

// todo when a file is changed, all inner files based on that need to be invalidated (editor only)
// done when a file is changed, the meta data of it needs to be invalidated
// idk only allocate each inner file once: create a static store of weak references

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

    abstract fun getChild(name: String): FileReference

    fun getChildOrNull(name: String): FileReference? =
        getChild(name).nullIfUndefined()

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
        return if (isValid) this else getReference(absolutePath)
    }

    /**
     * give access to an input stream;
     * should be buffered for better performance
     * */
    abstract fun inputStream(lengthLimit: Long = Long.MAX_VALUE, callback: (it: InputStream?, exc: Exception?) -> Unit)

    /**
     * give access to an output stream;
     * should be buffered for better performance
     * */
    @Throws(IOException::class)
    abstract fun outputStream(append: Boolean = false): OutputStream

    open fun readText(callback: (String?, Exception?) -> Unit) {
        readBytes { it, exc ->
            callback(if (it != null) String(it) else null, exc)
        }
    }

    open fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        inputStream { it, exc ->
            if (it != null) try {
                val bytes = it.readBytes()
                callback(bytes, null)
            } catch (e: Exception) {
                callback(null, e)
            } finally {
                it.close()
            } else callback(null, exc)
        }
    }

    @Throws(IOException::class)
    open fun inputStreamSync(): InputStream {
        var e: Exception? = null
        var d: InputStream? = null
        inputStream { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    @Throws(IOException::class)
    open fun readBytesSync(): ByteArray {
        var e: Exception? = null
        var d: ByteArray? = null
        readBytes { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    @Throws(IOException::class)
    open fun readTextSync(): String {
        var e: Exception? = null
        var d: String? = null
        readText { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    @Throws(IOException::class)
    open fun readByteBufferSync(native: Boolean): ByteBuffer {
        var e: Exception? = null
        var d: ByteBuffer? = null
        readByteBuffer(native) { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    open fun readByteBuffer(native: Boolean, callback: (ByteBuffer?, Exception?) -> Unit) {
        readBytes { bytes, exc ->
            if (bytes != null) {
                callback(
                    if (native) {
                        val buffer = ByteBufferPool.allocateDirect(bytes.size)
                        buffer.put(bytes).flip()
                        buffer
                    } else {
                        ByteBuffer.wrap(bytes)
                    }, null
                )
            } else callback(null, exc)
        }
    }

    open fun readLines(lineLengthLimit: Int, callback: (itr: ReadLineIterator?, exc: Exception?) -> Unit) {
        inputStream { it, exc ->
            if (it != null) {
                val reader = it.bufferedReader()
                callback(ReadLineIterator(reader, lineLengthLimit), null)
            } else callback(null, exc)
        }
    }

    @Throws(IOException::class)
    open fun readLinesSync(lineLengthLimit: Int): ReadLineIterator {
        var e: Exception? = null
        var d: ReadLineIterator? = null
        readLines(lineLengthLimit) { it, exc ->
            e = exc
            d = it
        }
        waitUntil(true) { e != null || d != null }
        return d ?: throw e!!
    }

    @Throws(IOException::class)
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

    @Throws(IOException::class)
    fun writeFile(file: FileReference, callback: (Exception?) -> Unit) {
        writeFile(file, { _, _ -> }, callback)
    }

    @Throws(IOException::class)
    open fun writeText(text: String) {
        val os = outputStream()
        val wr = OutputStreamWriter(os)
        wr.write(text)
        wr.close()
        os.close()
    }

    @Throws(IOException::class)
    open fun writeBytes(bytes: ByteArray) {
        val os = outputStream()
        os.write(bytes)
        os.close()
    }

    @Throws(IOException::class)
    open fun writeBytes(bytes: ByteBuffer) {
        val byte2 = ByteArray(bytes.remaining())
        val pos = bytes.position()
        bytes.get(byte2).position(pos)
        writeBytes(byte2)
    }

    @Throws(IOException::class)
    abstract fun length(): Long

    open fun toFile() = File(absolutePath.replace("!!", "/"))

    // fun length() = if (isInsideCompressed) zipFile?.size ?: 0L else file.length()
    fun openInExplorer() = toFile().openInExplorer()

    open fun relativePathTo(basePath: FileReference, maxNumBackPaths: Int): String? {
        if (maxNumBackPaths < 1 && !absolutePath.startsWith(basePath.absolutePath)) return null
        val parts = absolutePath.split('/')
        val baseParts = basePath.absolutePath.split('/')
        var matchingStartPaths = 0 // those can be skipped
        val ignoreCase = OS.isLinux || OS.isAndroid
        for (i in 0 until min(parts.size, baseParts.size)) {
            if (!parts[i].equals(baseParts[i], ignoreCase)) break
            matchingStartPaths++
        }
        if (parts.size - matchingStartPaths > maxNumBackPaths) return null
        // calculate size for result
        var resultSize = (baseParts.size - matchingStartPaths) * 3 - 1
        for (i in matchingStartPaths until parts.size) {
            resultSize += parts[i].length + 1
        }
        val result = StringBuilder(resultSize)
        for (i in matchingStartPaths until baseParts.size) {
            result.append("../")
        }
        for (i in matchingStartPaths until parts.size) {
            result.append(parts[i])
            if (i + 1 < parts.size) result.append('/')
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

    fun openInStandardProgram() {
        val parent = getParent()
        if (parent is InnerFile) return parent.openInStandardProgram()
        try {
            Desktop.getDesktop().open(toFile())
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
    }

    fun editInStandardProgram() {
        val parent = getParent()
        if (parent is InnerFile) return parent.editInStandardProgram()
        try {
            Desktop.getDesktop().edit(toFile())
        } catch (e: Exception) {
            LOGGER.warn(e.message)
            openInStandardProgram()
        }
    }

    @Throws(IOException::class)
    abstract fun delete(): Boolean

    @Throws(IOException::class)
    abstract fun mkdirs(): Boolean

    fun tryMkdirs(): Boolean {
        return try {
            mkdirs()
        } catch (e: Exception) {
            LOGGER.warn("Failed to create ${toString()}")
            false
        }
    }

    @Throws(IOException::class)
    open fun deleteOnExit() {
        deleteRecursively()
    }

    @Throws(IOException::class)
    open fun deleteRecursively(): Boolean {
        return delete()
    }

    val zipFileForDirectory
        get(): FileReference? {
            var zipFile = zipFile ?: return null
            if (!zipFile.isDirectory) {
                zipFile = InnerFolderCache.readAsFolder(zipFile, false) ?: return null
            }
            return zipFile
        }

    private val zipFile get() = InnerFolderCache.readAsFolder(this, false)

    abstract fun getParent(): FileReference

    fun getSibling(name: String): FileReference {
        return getParent().getChild(name)
    }

    fun getSiblingWithExtension(ext: String): FileReference {
        return getParent().getChild("$nameWithoutExtension.$ext")
    }

    @Throws(IOException::class)
    abstract fun renameTo(newName: FileReference): Boolean

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
        val signature = Signature.findNameSync(this)
        if (InnerFolderCache.hasReaderForFileExtension(lcExtension)) {
            return true
        }
        if (InnerFolderCache.hasReaderForSignature(signature)) {
            return true
        }
        return when (signature) { // todo these should be handled by InnerFolderCache...
            null, "xml", "json", "yaml" -> {// maybe something unknown, that we understand anyway
                // dae is xml
                when (lcExtension) {
                    in ThumbsExt.unityExtensions, "json" -> {
                        // LOGGER.info("Checking $absolutePath for mesh file, matches extension")
                        true
                    }
                    else -> false
                }
            }
            else -> {
                // LOGGER.info("Checking $absolutePath for zip file, other signature: $signature")
                false
            }
        }
    }

    abstract val exists: Boolean
    abstract val lastModified: Long
    abstract val lastAccessed: Long
    abstract val creationTime: Long

    abstract fun toUri(): URI

    override fun equals(other: Any?): Boolean {
        return other is FileReference && other._hashCode == _hashCode && other.absolutePath == absolutePath
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

    val windowsLnk: Lazy<WindowsShortcut?> = lazy {
        try {
            if (lcExtension == "lnk" && WindowsShortcut.isPotentialValidLink(this)) {
                WindowsShortcut.getSync(this)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    open val isSomeKindOfDirectory get() = isDirectory || windowsLnk.value != null || isPacked.value

    val isPacked = lazy {
        !isDirectory && isSerializedFolder()
    }

    open fun listChildren(): List<FileReference> {
        val link = windowsLnk.value
        if (link == null) {
            val folder = InnerFolderCache.readAsFolder(this, false)
            if (folder is InnerFolder) return folder.listChildren()
            if (folder != null) return listOf(folder)
            return emptyList()
        }
        // if the file is not a directory, then list the parent?
        // todo mark this child somehow?...
        val abs = link.absolutePath ?: return emptyList()
        val str = abs.replace('\\', '/')
        val ref = getReferenceOrTimeout(str)
        return listOf(
            if (link.isDirectory) {
                ref.getParent().ifUndefined(ref)
            } else ref
        )
    }

    open fun nullIfUndefined(): FileReference? = this

    open fun ifUndefined(other: FileReference): FileReference = this

    inline fun anyInHierarchy(run: (FileReference) -> Boolean): Boolean {
        var element = this
        while (element != InvalidRef) {
            if (run(this)) return true
            element = element.getParent() ?: return false
        }
        return false
    }

    open fun <V> toFile(run: (File) -> V, callback: (V?, Exception?) -> Unit) {
        val tmp = File.createTempFile(nameWithoutExtension, extension)
        readBytes { bytes, exc ->
            if (bytes != null) {
                tmp.writeBytes(bytes)
                val result = run(tmp)
                tmp.deleteOnExit()
                callback(result, null)
            } else {
                callback(null, exc)
            }
        }
    }

    fun printTree(depth: Int = 0) {
        LOGGER.info("${Tabs.spaces(depth * 2)}$name")
        if (isDirectory) {
            for (child in listChildren()) {
                child.printTree(depth + 1)
            }
        }
    }

    override fun destroy() {
    }

    // todo support for ffmpeg to read all zip files

}