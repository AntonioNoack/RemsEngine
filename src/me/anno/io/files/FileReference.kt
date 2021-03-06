package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.cache.instances.LastModifiedCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.gpu.GFX
import me.anno.io.unity.UnityReader
import me.anno.io.windows.WindowsShortcut
import me.anno.io.zip.InnerTmpFile
import me.anno.io.zip.ZipCache
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.min
import me.anno.studio.StudioBase
import me.anno.ui.editor.files.FileExplorer
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
import java.io.*
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.math.abs

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
    // todo other protocols as well, so like an URI replacement?

    companion object {

        private val LOGGER = LogManager.getLogger(FileReference::class)

        private val staticReferences = HashMap<String, FileReference>()

        private val fileCache = CacheSection("Files")
        var fileTimeout = 20_000L

        /**
         * removes old references
         * needs to be called regularly
         * */
        fun updateCache() {
            //allReferences.values.removeIf { it.get() == null }
        }

        fun register(ref: FileReference): FileReference {
            if (ref is FileFileRef) return ref
            fileCache.override(ref.absolutePath, CacheData(ref), fileTimeout)
            return ref
        }

        fun registerStatic(ref: FileReference): FileReference {
            staticReferences[ref.absolutePath] = ref
            return ref
        }

        /**
         * this happens rarely, and can be disabled in the shipping game
         * therefore it can be a little expensive
         * */
        fun invalidate(
            absolutePath: String
        ) {
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

        fun getReference(ref: FileReference): FileReference {
            return getReference(ref.absolutePath)
        }

        fun getReference(str: String?): FileReference {
            // invalid
            if (str == null || str.isBlank2()) return InvalidRef
            // root
            if (str == "root") return FileRootRef
            val str2 = str.replace('\\', '/')
            // the cache can be a large issue -> avoid if possible
            if (LastModifiedCache.exists(str2)) return createReference(str2)
            val data = fileCache.getEntry(str2, fileTimeout, false) {
                createReference(it)
            } as? FileReference // result may be null for unknown reasons; when this happens, use plan B
            return data ?: createReference(str)
        }

        fun getReferenceOrTimeout(str: String?, timeout: Long = 10_000): FileReference {
            if (str == null || str.isBlank2()) return InvalidRef
            val t1 = System.nanoTime() + timeout * MILLIS_TO_NANOS
            while (System.nanoTime() < t1) {
                val ref = getReferenceAsync(str)
                if (ref != null) return ref
            }
            return createReference(str)
        }

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

        private fun createReference(str: String): FileReference {
            // internal resource
            if (str.startsWith(BundledRef.prefix, true))
                return BundledRef.parse(str)

            if (str.startsWith("http://", true) ||
                str.startsWith("https://", true)
            ) return WebRef(str, emptyMap()) // todo string may contain parameters

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
            if (LastModifiedCache.exists(str)) return FileFileRef(File(str))
            // split by /, and check when we need to enter a zip file
            val parts = str.trim().split('/', '\\')

            // todo correct binary search here
            /*val cache = ExpensiveList(parts.size) { i ->
                val substr = parts.subList(0, i)
                    .joinToString("/")
                val file = File(substr)
                Pair(file, file.exists())
            }
            // we're searching for 0, but that will never be found
            val firstVirtualIndex = cache.findInsertIndex { if (it.second) -1 else +1 }
            val fileExists = cache[firstVirtualIndex - 1]
            if (fileExists.second) return appendPath(fileExists.first, firstVirtualIndex, parts)*/

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

        fun appendPath(parent: String, name: String): String {
            return if (parent.isBlank2()) name
            else "$parent/$name"
        }

        fun appendPath(fileI: File, i: Int, parts: List<String>): FileReference {
            var ref: FileReference = FileFileRef(fileI)
            for (j in i until parts.size) {
                ref = ref.getChild(parts[j])
                if (ref == InvalidRef) return ref
            }
            return ref
        }

        fun getReference(file: File?): FileReference {
            return getReference(file?.absolutePath?.replace('\\', '/'))
        }

        fun getReference(parent: File, name: String): FileReference {
            return getReference(getReference(parent), name)
        }

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

        fun createZipFile(file: FileReference): ZipFile {
            return if (file is FileFileRef) ZipFile(file.file) else
                ZipFile(SeekableInMemoryByteChannel(file.inputStream().readBytes()))
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
        ZipCache.unzipMaybe(this)?.invalidate()
    }

    fun validate(): FileReference {
        return if (isValid) this else getReference(absolutePath)
    }

    /**
     * give access to an input stream;
     * should be buffered for better performance
     * */
    abstract fun inputStream(): InputStream

    /**
     * give access to an output stream;
     * should be buffered for better performance
     * */
    abstract fun outputStream(): OutputStream

    open fun readText() = String(readBytes())
    open fun readText(charset: Charset) = String(readBytes(), charset)

    open fun readBytes() = inputStream().readBytes()
    fun readByteBuffer(native: Boolean): ByteBuffer {
        val bytes = readBytes()
        return if (native) {
            val buffer = ByteBufferPool.allocateDirect(bytes.size)
            buffer.put(bytes).flip()
            buffer
        } else {
            ByteBuffer.wrap(bytes)
        }
    }

    // todo read as sequence? :)
    fun readLines(): Iterable<String> = readText()
        .replace("\r", "")
        .split('\n')

    fun writeFile(file: FileReference, deltaProgress: (Long) -> Unit) {
        outputStream().use { output ->
            file.inputStream().use { input ->
                val buffer = ByteArray(2048)
                while (true) {
                    val numReadBytes = input.read(buffer)
                    if (numReadBytes < 0) break
                    output.write(buffer, 0, numReadBytes)
                    deltaProgress(numReadBytes.toLong())
                }
            }
        }
    }

    fun writeFile(file: FileReference) {
        outputStream().use { output ->
            file.inputStream().use { input ->
                val buffer = ByteArray(2048)
                while (true) {
                    val numReadBytes = input.read(buffer)
                    if (numReadBytes < 0) break
                    output.write(buffer, 0, numReadBytes)
                }
            }
        }
    }

    open fun writeText(text: String) {
        val os = outputStream()
        val wr = OutputStreamWriter(os)
        wr.write(text)
        wr.close()
        os.close()
    }

    open fun writeText(text: String, charset: Charset) {
        val os = outputStream()
        val wr = OutputStreamWriter(os, charset)
        wr.write(text)
        wr.close()
        os.close()
    }

    open fun writeBytes(bytes: ByteArray) {
        val os = outputStream()
        os.write(bytes)
        os.close()
    }

    open fun writeBytes(bytes: ByteBuffer) {
        val byte2 = ByteArray(bytes.remaining())
        bytes.get(byte2)
        if (!exists || length() != byte2.size.toLong() || !readBytes().contentEquals(byte2)) {
            writeBytes(byte2)
        }
    }

    abstract fun length(): Long

    open fun toFile() = File(absolutePath.replace("!!", "/"))

    // fun length() = if (isInsideCompressed) zipFile?.size ?: 0L else file.length()
    fun openInExplorer() = toFile().openInExplorer()

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

    abstract fun delete(): Boolean
    abstract fun mkdirs(): Boolean

    fun tryMkdirs(): Boolean {
        return try {
            mkdirs()
        } catch (e: java.lang.Exception) {
            LOGGER.warn("Failed to create ${toString()}")
            false
        }
    }

    open fun deleteOnExit() {
        deleteRecursively()
    }

    open fun deleteRecursively(): Boolean {
        return delete()
    }

    val zipFileForDirectory
        get(): FileReference? {
            var zipFile = zipFile ?: return null
            if (!zipFile.isDirectory) {
                zipFile = ZipCache.unzip(zipFile, false) ?: return null
            }
            return zipFile
        }

    private val zipFile get() = ZipCache.unzip(this, false)

    abstract fun getParent(): FileReference?

    fun getSibling(name: String): FileReference {
        return getParent()?.getChild(name) ?: InvalidRef
    }

    fun renameTo(newName: File) = renameTo(getReference(newName))
    abstract fun renameTo(newName: FileReference): Boolean

    abstract val isDirectory: Boolean

    open fun isSerializedFolder(): Boolean {
        // only read the first bytes
        val signature = Signature.findName(this)
        if (ZipCache.hasReaderForFileExtension(lcExtension)) {
            LOGGER.info("Checking $absolutePath for zip/similar file, matches extension")
            return true
        }
        if (ZipCache.hasReaderForSignature(signature)) {
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
                        val zis = createZipFile(this)
                        val result = zis.entries.hasMoreElements()
                        LOGGER.info("Checking $absolutePath for zip file, success")
                        result
                    } catch (e: IOException) {
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

    fun <V> toFile(run: (File) -> V): V {
        return if (this is FileFileRef) {
            run(file)
        } else {
            val tmp = File.createTempFile(nameWithoutExtension, extension)
            tmp.writeBytes(readBytes())
            val result = run(tmp)
            tmp.deleteOnExit()
            result
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