package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.utils.InternalAPI
import me.anno.utils.OS
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object Reference {

    @JvmField
    val invalidateListeners = ArrayList<(absolutePath: String) -> Unit>()

    @JvmStatic
    private val LOGGER = LogManager.getLogger(FileReference::class)

    @JvmStatic
    private val staticReferences = HashMap<String, FileReference>()

    @JvmStatic
    private val fileCache = CacheSection<String, FileReference>("Files")

    @JvmField
    var fileTimeout = 20_000L

    @JvmStatic
    fun registerStatic(ref: FileReference): FileReference {
        staticReferences[ref.absolutePath] = ref
        return ref
    }

    @JvmStatic
    @InternalAPI // idk, do you need this?
    @Suppress("unused") // JVM2WASM needs it
    fun queryStatic(absolutePath: String): FileReference? {
        return staticReferences[absolutePath]
    }

    /**
     * this happens rarely, and can be disabled in the shipping game
     * therefore it can be a little expensive
     * */
    @JvmStatic
    fun invalidate(absolutePath: String) {
        LOGGER.info("Invalidating $absolutePath")
        fileCache.removeIf { key, _ -> key.startsWith(absolutePath) }
        for (li in invalidateListeners.indices) {
            invalidateListeners[li](absolutePath)
        }
    }

    /** keep the value loaded and check if it has changed maybe (internal files, like zip files) */
    @JvmStatic
    fun getReference(ref: FileReference, timeoutMillis: Long = fileTimeout): FileReference {
        fileCache.getEntryWithoutGenerator(ref.absolutePath, timeoutMillis)
        return ref.validate()
    }

    @JvmStatic
    fun getReference(rawPath: String?): FileReference {
        // handling a few special cases:
        if (rawPath == null || rawPath.isBlank2()) return InvalidRef
        if (rawPath == "root") return FileRootRef
        if (rawPath == BundledRef.PREFIX) return BundledRef.origin
        // proper resolution
        val path = sanitizePath(rawPath)
        return createQuickReference(path)
    }

    @JvmStatic
    fun getRealReference(path: String): FileReference {
        return fileCache.getEntry(path, fileTimeout, generator)
            .waitFor("getRealReference") ?: createReference(path)
    }

    @JvmStatic
    fun getRealReferenceOrNull(path: String): FileReference? {
        val fromCache = fileCache.getEntry(path, fileTimeout, generator).value
        return if (fromCache != null && fromCache.absolutePath == path) fromCache else null
    }

    private val generator = { path: String, result: AsyncCacheData<FileReference> ->
        result.value = createReference(path)
    }
    private val linkRefCache = ConcurrentHashMap<String, LinkFileReference>()

    @JvmStatic
    private fun getPrefixIdx(str2: String): Int {
        val prefixIdx = str2.indexOf("://")
        return when {
            prefixIdx >= 0 -> prefixIdx + 3 // protocol prefix
            str2.length >= 2 && str2.startsWith("//") -> 2
            str2.length >= 3 && str2[1] == ':' && str2[2] == '/' -> 2 // drive letter prefix
            else -> 0
        }
    }

    @JvmStatic
    private fun needsRelativePathReplacements(str2: String): Boolean {
        return "/../" in str2 || str2.endsWith("/..") ||
                "/./" in str2 || str2.endsWith("/.") || str2.startsWith("./") ||
                "//" in str2 || str2.startsWith("/") || str2.endsWith("/")
    }

    @JvmStatic
    private fun replaceRelativePaths(str2: String, prefixIdx: Int): String {
        val parts = str2
            .substring(prefixIdx)
            .split('/')
            .toMutableList()
        var i = 0
        while (i < parts.size) {
            val part = parts[i]
            when (part) {
                ".." -> {
                    if (i > 0 && parts[i - 1] != "..") {
                        parts.removeAt(i)
                        parts.removeAt(i - 1)
                        i--
                    } else {
                        RuntimeException("../ at the start cannot be sanitized ('$str2')")
                            .printStackTrace()
                        i++
                    }
                }
                ".", "" -> parts.removeAt(i)
                else -> i++
            }
        }
        val prefix = str2.substring(0, prefixIdx)
        return parts.joinToString("/", prefix)
    }

    @JvmStatic
    fun sanitizePath(str: String): String {
        val str2 = if ('\\' in str) str.replace('\\', '/') else str
        val str3 = str2.trim() // is this creating a new string if nothing needs to be done?
        val str4 = if (needsRelativePathReplacements(str3)) {
            val prefixIdx = getPrefixIdx(str3)
            replaceRelativePaths(str3, prefixIdx)
        } else str3
        return str4
    }

    @JvmStatic
    private fun createReference(absolutePath: String): FileReference {

        // internal resource
        val bundledRef = BundledRef.parse(absolutePath)
        if (bundledRef != null) return bundledRef

        // web resource
        if (isWebResource(absolutePath)) {
            return createWebRef(absolutePath)
        }

        // runtime-only resource
        if (isTemporaryFile(absolutePath)) {
            return resolveTemporaryFile(absolutePath)
        }

        // resource, which is defined by always-existing object
        val staticRef = staticReferences[absolutePath]
        if (staticRef != null) return staticRef

        // real or compressed files
        // check whether it exists -> easy then :)
        if (LastModifiedCache.exists(absolutePath)) {
            return createFileFileRef(absolutePath)
        }

        if (OS.isWindows) {
            if (!isAllowedWindowsPath(absolutePath)) {
                LOGGER.warn("Invalid file! '$absolutePath'")
                return InvalidRef
            }
        }

        return createReferenceOnLastExistingFile(absolutePath)
    }

    @JvmStatic
    private fun createWebRef(absolutePath: String): FileReference {
        return WebRef(absolutePath, emptyMap())
    }

    @JvmStatic
    private fun createReferenceOnLastExistingFile(absolutePath: String): FileReference {
        // split by /, and check when we need to enter a zip file
        val parts = absolutePath.split('/')
        val builder = StringBuilder(absolutePath)
        for (i in parts.lastIndex downTo 0) {
            val substr = builder.toString()
            if (i < parts.lastIndex && LastModifiedCache.exists(substr)) {
                // great :), now go into that file
                return appendPath(substr, i + 1, parts)
            }
            if (i > 0) {
                var newLength = builder.length - (parts[i].length + 1)
                // keep slash for driver-letter
                if (newLength == 2 && builder[newLength - 1] == ':') newLength++
                builder.setLength(newLength)
            } // else builder no longer needed
        }

        return createFallbackReference(absolutePath)
    }

    @JvmStatic
    private fun appendPath(substr: String, i: Int, parts: List<String>): FileReference {
        return appendPath(File(substr), i, parts)
    }

    @JvmStatic
    private fun createFallbackReference(absolutePath: String): FileReference {
        // somehow, we could not find the correct file
        // it probably just is new
        LOGGER.warn("Could not find correct sub file for $absolutePath")
        return FileFileRef(File(absolutePath))
    }

    @JvmStatic
    private fun createQuickReference(absolutePath: String): FileReference {

        // runtime-only resource
        if (isTemporaryFile(absolutePath)) {
            return resolveTemporaryFile(absolutePath)
        }

        // resource, which is defined by always-existing object
        val static = staticReferences[absolutePath]
        if (static != null) return static

        return linkRefCache.getOrPut(absolutePath) {
            LinkFileReference(absolutePath)
        }
    }

    @JvmStatic
    private fun isWebResource(absolutePath: String): Boolean {
        return absolutePath.startsWith("http://", true) || absolutePath.startsWith("https://", true)
    }

    @JvmStatic
    private fun isTemporaryFile(absolutePath: String): Boolean {
        return absolutePath.startsWith(InnerTmpFile.PREFIX)
    }

    @JvmStatic
    private fun resolveTemporaryFile(absolutePath: String): FileReference {
        val tmp = InnerTmpFile.find(absolutePath)
        if (tmp == null) LOGGER.warn("$absolutePath could not be found, maybe it was created in another session, or GCed")
        return tmp ?: InvalidRef
    }

    @JvmStatic
    fun isWindowsDriveLetterWithoutSlash(absolutePath: String): Boolean {
        return absolutePath.length == 2 && absolutePath[1] == ':' &&
                (absolutePath[0] in 'A'..'Z' || absolutePath[0] in 'a'..'z')
    }

    @JvmStatic
    fun isWindowsUNCPath(absolutePath: String): Boolean {
        return absolutePath.startsWith("//")
    }

    @JvmStatic
    fun isAllowedWindowsPath(absolutePath: String): Boolean {
        return isWindowsDriveLetterWithoutSlash(absolutePath) ||
                isWindowsUNCPath(absolutePath) ||
                ":/" in absolutePath
    }

    @JvmStatic
    private fun createFileFileRef(absolutePath: String): FileReference {
        val pathname = if (isWindowsDriveLetterWithoutSlash(absolutePath)) "$absolutePath/" else absolutePath
        return FileFileRef(File(pathname))
    }

    @JvmStatic
    private fun appendPath(ref0: FileReference, i: Int, names: List<String>): FileReference {
        var ref = ref0
        for (j in i until names.size) {
            ref = ref.getChildImpl(names[j])
            if (ref == InvalidRef) return ref
        }
        return ref
    }

    @JvmStatic
    fun appendPath(fileI: File, i: Int, parts: List<String>): FileReference {
        return appendPath(FileFileRef(fileI), i, parts)
    }

    @JvmStatic
    fun appendPath(parent: String, name: String): String {
        val pe = parent.endsWith("/")
        val ne = name.startsWith("/")
        return when {
            parent.isBlank2() -> name
            name == "/" -> parent
            pe && ne -> "${parent.substring(0, parent.lastIndex)}$name"
            pe || ne -> "$parent$name"
            else -> "$parent/$name"
        }
    }

    fun getParent(absolutePath: String): FileReference {
        if (absolutePath.endsWith("://")) return FileRootRef
        var index = absolutePath.lastIndexOf('/')
        return if (index < 0) {
            if (absolutePath == FileRootRef.name) InvalidRef
            else FileRootRef
        } else {
            assertTrue(index != absolutePath.lastIndex, absolutePath)
            if (index >= 2 && absolutePath[index - 1] == '/' && absolutePath[index - 2] == ':') index++
            getReference(absolutePath.substring(0, index))
        }
    }
}