package me.anno.io.files

import me.anno.io.windows.WindowsShortcut
import me.anno.studio.StudioBase
import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.files.LocalFile.toLocalPath
import me.anno.utils.types.Strings.isBlank2
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.URI
import java.nio.charset.Charset

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
abstract class FileReference(val absolutePath: String) {

    // done if there is a !!, it's into a zip file -> it only needs to be a slash;
    // all zip files should be detected automatically
    // todo if res:// at the start (?), then it's a local resource
    // todo other protocols as well, so like an URI replacement?

    companion object {

        private val LOGGER = LogManager.getLogger(FileReference::class)

        fun getReference(str: String): FileReference {
            if (str.isBlank2()) return InvalidRef
            if (str == "null") return FileRootRef
            return FileFileRef(File(str))
        }

        fun getReference(file: File?): FileReference {
            if (file == null) return FileRootRef
            val str = file.toString()
            if (str.isBlank2()) return InvalidRef
            if (str == "null") return FileRootRef
            return FileFileRef(file)
        }

        fun getReference(parent: File, name: String): FileReference {
            return getReference(getReference(parent), name)
        }

        fun getReference(parent: FileReference?, name: String): FileReference {
            parent ?: return InvalidRef
            var result: FileReference? = parent
            val name1 = name.replace('\\', '/')
            return if ('/' !in name1) {
                result?.getChild(name)
            } else {
                val parts = name1.split('/')
                for (partialName in parts) {
                    if (!partialName.isBlank2()) {
                        result = if (partialName == "..") {
                            result?.getParent()
                        } else {
                            result?.getChild(partialName)
                        }
                    }
                }
                result
            } ?: InvalidRef
        }

    }

    abstract fun getChild(name: String): FileReference

    /*constructor() : this("")
    constructor(parent: File, name: String) : this(File(parent, name))
    constructor(parent: FileReference, name: String) : this(File(parent.file, name))

    constructor(str: String) : this(File(str))*/

    // val nameWithoutExtension = file.nameWithoutExtension
    // val extension = file.extension

    val name: String
    val nameWithoutExtension: String
    val extension: String

    init {
        val lastIndex = absolutePath.lastIndexOf('/')
        name = if (lastIndex < 0) {
            absolutePath
        } else absolutePath.substring(lastIndex + 1)
        val extIndex = name.lastIndexOf('.')
        if (extIndex < 0) {
            extension = ""
            nameWithoutExtension = name
        } else {
            extension = name.substring(extIndex + 1)
            nameWithoutExtension = name.substring(0, extIndex)
        }
    }

    val hashCode = absolutePath.hashCode()

    val hasValidName = !absolutePath.isBlank2()
    fun hasValidName() = hasValidName

    abstract fun inputStream(): InputStream

    abstract fun outputStream(): OutputStream

    fun readText() = String(readBytes())
    fun readText(charset: Charset) = String(readBytes(), charset)

    fun readBytes() = inputStream().readBytes()

    fun writeText(text: String) {
        val os = outputStream()
        val wr = OutputStreamWriter(os)
        wr.write(text)
        wr.close()
        os.close()
    }

    fun writeText(text: String, charset: Charset) {
        val os = outputStream()
        val wr = OutputStreamWriter(os, charset)
        wr.write(text)
        wr.close()
        os.close()
    }

    fun writeBytes(bytes: ByteArray) {
        val os = outputStream()
        os.write(bytes)
        os.close()
    }

    abstract fun length(): Long

    // fun length() = if (isInsideCompressed) zipFile?.size ?: 0L else file.length()
    fun openInExplorer() = File(absolutePath.replace("!!", "/")).openInExplorer()

    abstract fun deleteRecursively(): Boolean
    abstract fun deleteOnExit()
    abstract fun delete(): Boolean
    abstract fun mkdirs(): Boolean

    val zipFileForDirectory
        get(): FileReference? {
            var zipFile = zipFile ?: return null
            if (!zipFile.isDirectory) {
                zipFile = ZipCache.getMeta2(zipFile, false) ?: return null
            }
            return zipFile
        }

    private val zipFile get() = ZipCache.getMeta2(this, false)

    abstract fun getParent(): FileReference?

    fun renameTo(newName: File) = renameTo(getReference(newName))
    abstract fun renameTo(newName: FileReference): Boolean

    abstract val isDirectory: Boolean

    private fun isZipFile(): Boolean {
        // todo only read the first bytes...
        if (extension.equals("7z", true)) {
            val zis = if (this is FileFileRef) SevenZFile(file) else
                SevenZFile(SeekableInMemoryByteChannel(inputStream().readBytes()))
            return try {
                val result = zis.nextEntry != null
                LOGGER.info("Checking $absolutePath for zip file, success")
                result
            } catch (e: IOException) {
                LOGGER.info("Checking $absolutePath for zip file, ${e.message}")
                false
            } finally {
                zis.close()
            }
        } else {
            return try {
                val zis = if (this is FileFileRef) ZipFile(file) else
                    ZipFile(SeekableInMemoryByteChannel(inputStream().readBytes()))
                val result = zis.entries.hasMoreElements()
                LOGGER.info("Checking $absolutePath for zip file, success")
                result
            } catch (e: IOException) {
                LOGGER.info("Checking $absolutePath for zip file, ${e.message}")
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

    @Deprecated(
        "This function only is defined, if the reference is an actual file",
        ReplaceWith("inputStream(),outputStream()")
    )

    val unsafeFile
        get() = (this as FileFileRef).file

    fun toLocalPath(workspace: FileReference? = StudioBase.workspace): String {
        return absolutePath.toLocalPath(workspace)
    }

    val windowsLnk: Lazy<WindowsShortcut?> = lazy {
        if (extension.equals("lnk", true) && WindowsShortcut.isPotentialValidLink(this)) {
            try {
                WindowsShortcut(this)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    val isSomeKindOfDirectory get() = isDirectory || windowsLnk.value != null || isPacked.value

    val isPacked = lazy {
        !isDirectory && isZipFile()
    }

    open fun listChildren(): List<FileReference>? {
        // println("listing children of $this, lnk: ${windowsLnk.value}")
        val link = windowsLnk.value ?: return null
        // if the file is not a directory, then list the parent?
        // todo mark this child somehow?...
        val str = link.realFilename.replace('\\', '/')
        val ref = getReference(str)
        return listOf(
            if (link.isDirectory) {
                ref.getParent() ?: ref
            } else ref
        )
    }

    // todo support for ffmpeg to read all zip files

}