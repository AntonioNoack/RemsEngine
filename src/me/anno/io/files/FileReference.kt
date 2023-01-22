package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.cache.instances.LastModifiedCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.gpu.GFX
import me.anno.io.unity.UnityReader
import me.anno.io.utils.WindowsShortcut
import me.anno.io.zip.InnerFolderCache
import me.anno.io.zip.InnerTmpFile
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.min
import me.anno.studio.StudioBase
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Tabs
import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.files.LocalFile.toLocalPath
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.strings.StringHelper.indexOf2
import me.anno.utils.types.Strings.isBlank2
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset

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

    // done if there is a !!, it's into a zip file -> it only needs to be a slash;
    // all zip files should be detected automatically
    // done if res:// at the start, then it's a local resource

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FileReference::class)

        @JvmStatic
        private val staticReferences = HashMap<String, FileReference>()

        @JvmStatic
        private val fileCache = CacheSection("Files")

        @JvmField
        var fileTimeout = 20_000L

        /**
         * removes old references
         * needs to be called regularly
         * */
        @JvmStatic
        fun updateCache() {
            //allReferences.values.removeIf { it.get() == null }
        }

        @JvmStatic
        fun register(ref: FileReference): FileReference {
            if (ref is FileFileRef) return ref
            fileCache.override(ref.absolutePath, CacheData(ref), fileTimeout)
            return ref
        }

        @JvmStatic
        fun registerStatic(ref: FileReference): FileReference {
            staticReferences[ref.absolutePath] = ref
            return ref
        }

        /**
         * this happens rarely, and can be disabled in the shipping game
         * therefore it can be a little expensive
         * */
        @JvmStatic
        fun invalidate(absolutePath: String) {
            LOGGER.info("Invalidating $absolutePath")
            val path = absolutePath.replace('\\', '/')
            synchronized(fileCache) {
                fileCache.remove { key, _ ->
                    key is String && key.startsWith(path)
                }
            }
            // go over all file explorers, and invalidate them, if they contain it, or are inside
            // a little unspecific; works anyway
            val parent = getReferenceOrTimeout(absolutePath).getParent()
            if (parent != null && parent != InvalidRef) {
                // todo we should invalidate ALL windowStacks
                for (window0 in GFX.windows) {
                    for (window in window0.windowStack) {
                        try {
                            window.panel.forAll {
                                if (it is FileExplorer && it.folder
                                        .absolutePath
                                        .startsWith(parent.absolutePath)
                                ) {
                                    it.invalidate()
                                }
                            }
                        } catch (e: Exception) {
                            // this is not on the UI thread, so the UI may change, and cause
                            // index out of bounds exceptions
                            e.printStackTrace()
                        }
                    }
                }
            }
            val removed = PrefabCache.removeDual { file, _, _ ->
                (file is FileReference && file.absolutePath.startsWith(path, true)) ||
                        (file is String && file.startsWith(path, true))
            }
            LOGGER.info("Removed $removed instances from prefab cache")
        }

        /** keep the value loaded and check if it has changed maybe (internal files, like zip files) */
        @JvmStatic
        fun getReference(ref: FileReference, timeoutMillis: Long = fileTimeout): FileReference {
            fileCache.getEntryWithoutGenerator(ref.absolutePath, timeoutMillis)
            return ref.validate()
        }

        @JvmStatic
        fun getReference(str: String?): FileReference {
            // invalid
            if (str == null || str.isBlank2()) return InvalidRef
            // root
            if (str == "root") return FileRootRef
            val str2 = if ('\\' in str) str.replace('\\', '/') else str
            // the cache can be a large issue -> avoid if possible
            if (LastModifiedCache.exists(str2)) {
                return createReference(str2)
            }
            val data = fileCache.getEntry(str2, fileTimeout, false) {
                createReference(it)
            } as? FileReference // result may be null for unknown reasons; when this happens, use plan B
            return data ?: createReference(str)
        }

        @JvmStatic
        fun getReferenceOrTimeout(str: String?, timeout: Long = 10_000): FileReference {
            if (str == null || str.isBlank2()) return InvalidRef
            val t1 = System.nanoTime() + timeout * MILLIS_TO_NANOS
            while (System.nanoTime() < t1) {
                val ref = getReferenceAsync(str)
                if (ref != null) return ref
            }
            return createReference(str)
        }

        @JvmStatic
        fun getReferenceAsync(str: String?): FileReference? {
            // invalid
            if (str == null || str.isBlank2()) return InvalidRef
            // root
            if (str == "root") return FileRootRef
            val str2 = str.replace('\\', '/')
            // the cache can be a large issue -> avoid if possible
            if (LastModifiedCache.exists(str2)) return createReference(str2)
            return fileCache.getEntry(str2, fileTimeout, true) {
                createReference(it)
            } as? FileReference
        }

        @JvmStatic
        private fun createReference(str: String): FileReference {

            // internal resource
            if (str.startsWith(BundledRef.prefix, true))
                return BundledRef.parse(str)

            if (str.startsWith("http://", true) ||
                str.startsWith("https://", true)
            ) return WebRef(str, emptyMap())

            if (str.startsWith("tmp://")) {
                val tmp = InnerTmpFile.find(str)
                if (tmp == null) LOGGER.warn("$str could not be found, maybe it was created in another session, or GCed")
                return tmp ?: InvalidRef
            }

            // static references
            val static = staticReferences[str]
            if (static != null) return static

            // real or compressed files
            // check whether it exists -> easy then :)
            if (LastModifiedCache.exists(str)) {
                val str2 = if (str.length == 2 && str[1] == ':' &&
                    (str[0] in 'A'..'Z' || str[0] in 'a'..'z')
                ) "$str/" else str
                return FileFileRef(File(str2))
            }

            // split by /, and check when we need to enter a zip file
            val parts = str.trim().split('/', '\\')

            // binary search? let's do linear first
            for (i in parts.lastIndex downTo 0) {
                val substr = parts.subList(0, i).joinToString("/")
                if (LastModifiedCache.exists(substr)) {
                    // great :), now go into that file
                    return appendPath(File(substr), i, parts)
                }
            }
            // somehow, we could not find the correct file
            // it probably just is new
            LOGGER.warn("Could not find correct sub file for $str")
            return FileFileRef(File(str))
        }

        @JvmStatic
        fun appendPath(parent: String, name: String): String {
            return if (parent.isBlank2()) name
            else "$parent/$name"
        }

        @JvmStatic
        fun appendPath(ref0: FileReference, i: Int, parts: List<String>): FileReference {
            var ref = ref0
            for (j in i until parts.size) {
                ref = ref.getChild(parts[j])
                if (ref == InvalidRef) return ref
            }
            return ref
        }

        @JvmStatic
        fun appendPath(fileI: File, i: Int, parts: List<String>) =
            appendPath(FileFileRef(fileI), i, parts)

        @JvmStatic
        fun getReference(file: File?): FileReference {
            return getReference(file?.absolutePath?.replace('\\', '/'))
        }

        @JvmStatic
        fun getReference(parent: File, name: String): FileReference {
            return getReference(getReference(parent), name)
        }

        @JvmStatic
        fun getReference(parent: FileReference?, name: String): FileReference {
            var result = parent ?: return InvalidRef
            if ('/' !in name && '\\' !in name) {
                return result.getChild(name)
            } else {
                val parts = name.split('/', '\\')
                for (partialName in parts) {
                    if (!partialName.isBlank2()) {
                        result = if (partialName == "..") {
                            result.getParent()
                        } else {
                            result.getChild(partialName)
                        } ?: return InvalidRef
                    }
                }
                return result
            }
        }

        @JvmStatic
        fun createZipFile(file: FileReference, callback: (ZipFile?, Exception?) -> Unit) {
            return if (file is FileFileRef) callback(ZipFile(file.file), null) else {
                file.readBytes { it, exc ->
                    if (it != null) {
                        callback(ZipFile(SeekableInMemoryByteChannel(it)), null)
                    } else callback(null, exc)
                }
            }
        }

    }

    private var isValid = true

    /*constructor() : this("")
    constructor(parent: File, name: String) : this(File(parent, name))
    constructor(parent: FileReference, name: String) : this(File(parent.file, name))

    constructor(str: String) : this(File(str))*/

    // val nameWithoutExtension = file.nameWithoutExtension
    // val extension = file.extension

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

    val hashCode = absolutePath.hashCode()

    val hasValidName = !absolutePath.isBlank2()
    fun hasValidName() = hasValidName

    var isHidden =
        name.startsWith('.') || (lcExtension == "meta" && "/Assets/" in absolutePath) // hidden file in Linux, or file in unity package

    fun hide() {
        isHidden = true
    }

    abstract fun getChild(name: String): FileReference

    fun getChildOrNull(name: String): FileReference? =
        getChild(name).nullIfUndefined()

    open fun hasChildren(): Boolean = listChildren()?.isNotEmpty() == true

    open fun invalidate() {
        LOGGER.info("Invalidated $absolutePath")
        isValid = false
        // if this has inner folders, replace all of their children as well
        InnerFolderCache.wasReadAsFolder(this)?.invalidate()
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
    @kotlin.jvm.Throws(IOException::class)
    abstract fun outputStream(append: Boolean = false): OutputStream

    open fun readText(callback: (String?, Exception?) -> Unit) {
        readText(Charset.forName("UTF-8"), callback)
    }

    open fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        readBytes { it, exc ->
            callback(if (it != null) String(it, charset) else null, exc)
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

    @kotlin.jvm.Throws(IOException::class)
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

    @kotlin.jvm.Throws(IOException::class)
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

    @kotlin.jvm.Throws(IOException::class)
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

    @kotlin.jvm.Throws(IOException::class)
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

    @kotlin.jvm.Throws(IOException::class)
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

    @kotlin.jvm.Throws(IOException::class)
    fun writeFile(file: FileReference, deltaProgress: (Long) -> Unit, callback: (Exception?) -> Unit) {
        outputStream().use { output ->
            file.inputStream { input, exc ->
                if (input != null) {
                    val buffer = ByteArray(2048)
                    while (true) {
                        val numReadBytes = input.read(buffer)
                        if (numReadBytes < 0) break
                        output.write(buffer, 0, numReadBytes)
                        deltaProgress(numReadBytes.toLong())
                    }
                    callback(null)
                } else callback(exc)
            }
        }
    }

    @kotlin.jvm.Throws(IOException::class)
    fun writeFile(file: FileReference, callback: (Exception?) -> Unit) {
        writeFile(file, {}, callback)
    }

    @kotlin.jvm.Throws(IOException::class)
    open fun writeText(text: String) {
        val os = outputStream()
        val wr = OutputStreamWriter(os)
        wr.write(text)
        wr.close()
        os.close()
    }

    @kotlin.jvm.Throws(IOException::class)
    open fun writeText(text: String, charset: Charset) {
        val os = outputStream()
        val wr = OutputStreamWriter(os, charset)
        wr.write(text)
        wr.close()
        os.close()
    }

    @kotlin.jvm.Throws(IOException::class)
    open fun writeBytes(bytes: ByteArray) {
        val os = outputStream()
        os.write(bytes)
        os.close()
    }

    @kotlin.jvm.Throws(IOException::class)
    open fun writeBytes(bytes: ByteBuffer) {
        val byte2 = ByteArray(bytes.remaining())
        bytes.get(byte2)
        // readBytes() may potentially be awfully slow
        if (!exists || length() != byte2.size.toLong()/* || !readBytes().contentEquals(byte2)*/) {
            writeBytes(byte2)
        }
    }

    @kotlin.jvm.Throws(IOException::class)
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
            if (children != null) for (child in children) {
                val r = child.findRecursively(maxDepth - 1, find)
                if (r != null) return r
            }
        }
        return null
    }

    fun openInStandardProgram() {
        try {
            Desktop.getDesktop().open(toFile())
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
    }

    fun editInStandardProgram() {
        try {
            Desktop.getDesktop().edit(toFile())
        } catch (e: Exception) {
            LOGGER.warn(e.message)
            openInStandardProgram()
        }
    }

    @kotlin.jvm.Throws(IOException::class)
    abstract fun delete(): Boolean

    @kotlin.jvm.Throws(IOException::class)
    abstract fun mkdirs(): Boolean

    fun tryMkdirs(): Boolean {
        return try {
            mkdirs()
        } catch (e: Exception) {
            LOGGER.warn("Failed to create ${toString()}")
            false
        }
    }

    @kotlin.jvm.Throws(IOException::class)
    open fun deleteOnExit() {
        deleteRecursively()
    }

    @kotlin.jvm.Throws(IOException::class)
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

    abstract fun getParent(): FileReference?

    fun getSibling(name: String): FileReference {
        return getParent()?.getChild(name) ?: InvalidRef
    }

    fun renameTo(newName: File) = renameTo(getReference(newName))

    @kotlin.jvm.Throws(IOException::class)
    abstract fun renameTo(newName: FileReference): Boolean

    abstract val isDirectory: Boolean

    open fun isSerializedFolder(): Boolean {
        // only read the first bytes
        val signature = Signature.findNameSync(this)
        if (InnerFolderCache.hasReaderForFileExtension(lcExtension)) {
            LOGGER.info("Checking $absolutePath for zip/similar file, matches extension")
            return true
        }
        if (InnerFolderCache.hasReaderForSignature(signature)) {
            LOGGER.info("Checking $absolutePath for zip/similar file, matches signature")
            return true
        }
        return when (signature) {
            null, "xml", "json", "yaml" -> {// maybe something unknown, that we understand anyway
                // dae is xml
                when (lcExtension) {
                    in UnityReader.unityExtensions, "json" -> {
                        LOGGER.info("Checking $absolutePath for mesh file, matches extension")
                        true
                    }
                    else -> try {
                        var zis: ZipFile? = null
                        var e: Exception? = null
                        createZipFile(this) { z, exc ->
                            zis = z
                            e = exc
                        }
                        waitUntil(true) { zis != null || e != null }
                        val result = (zis ?: throw e!!).entries.hasMoreElements()
                        LOGGER.info("Checking $absolutePath for zip file, success")
                        result
                    } catch (e: Exception) {
                        LOGGER.info("Checking $absolutePath for zip file, ${e.message}")
                        false
                    }
                }
            }
            else -> {
                LOGGER.info("Checking $absolutePath for zip file, other signature: $signature")
                false
            }
        }
    }

    abstract val exists: Boolean
    abstract val lastModified: Long
    abstract val lastAccessed: Long

    abstract fun toUri(): URI

    override fun equals(other: Any?): Boolean {
        return other is FileReference && other.hashCode == hashCode && other.absolutePath == absolutePath
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun toString(): String {
        return absolutePath
    }

    open fun toLocalPath(workspace: FileReference? = StudioBase.workspace): String {
        return absolutePath.toLocalPath(workspace)
    }

    val windowsLnk: Lazy<WindowsShortcut?> = lazy {
        if (lcExtension == "lnk" && WindowsShortcut.isPotentialValidLink(this)) {
            try {
                WindowsShortcut(this)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    open val isSomeKindOfDirectory get() = isDirectory || windowsLnk.value != null || isPacked.value

    val isPacked = lazy {
        !isDirectory && isSerializedFolder()
    }

    open fun listChildren(): List<FileReference>? {
        // LOGGER.info("listing children of $this, lnk: ${windowsLnk.value}")
        val link = windowsLnk.value ?: return null
        // if the file is not a directory, then list the parent?
        // todo mark this child somehow?...
        val abs = link.absolutePath ?: return null
        val str = abs.replace('\\', '/')
        val ref = getReferenceOrTimeout(str)
        return listOf(
            if (link.isDirectory) {
                ref.getParent() ?: ref
            } else ref
        )
    }

    open fun nullIfUndefined(): FileReference? = this

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

    private fun printTree(depth: Int = 0) {
        LOGGER.info("${Tabs.spaces(depth * 2)}$name")
        if (isDirectory) {
            for (child in listChildren() ?: return) {
                child.printTree(depth + 1)
            }
        }
    }

    override fun destroy() {
    }

    // todo support for ffmpeg to read all zip files

}