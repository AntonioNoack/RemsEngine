package me.anno.io.files

import me.anno.Time
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.maths.Maths
import me.anno.utils.InternalAPI
import me.anno.utils.OS
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.File

object Reference {

    @JvmField
    val invalidateListeners = ArrayList<(absolutePath: String) -> Unit>()

    @JvmStatic
    private val LOGGER = LogManager.getLogger(FileReference::class)

    @JvmStatic
    private val staticReferences = HashMap<String, FileReference>()

    @JvmStatic
    private val fileCache = CacheSection("Files")

    @JvmField
    var fileTimeout = 20_000L

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
        fileCache.remove { key, _ ->
            key is String && key.startsWith(absolutePath)
        }
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
        // invalid
        if (rawPath == null || rawPath.isBlank2()) return InvalidRef
        // root
        if (rawPath == "root") return FileRootRef
        val path = sanitizePath(rawPath)
        return createQuickReference(path)
    }

    @JvmStatic
    fun getRealReference(path: String): FileReference {
        return fileCache.getEntry(path, fileTimeout, false, generator)
            ?: createReference(path)
    }

    private val generator = { path: String -> createReference(path) }

    @JvmStatic
    fun getReferenceOrTimeout(str: String?, timeoutMillis: Long = 10_000): FileReference {
        if (str == null || str.isBlank2()) return InvalidRef
        val t1 = Time.nanoTime + timeoutMillis * Maths.MILLIS_TO_NANOS
        while (Time.nanoTime < t1) {
            val ref = getReferenceAsync(str)
            if (ref != null) return ref
        }
        return createReference(str)
    }

    @JvmStatic
    fun getReferenceAsync(rawPath: String?): FileReference? {
        // invalid
        if (rawPath == null || rawPath.isBlank2()) return InvalidRef
        // root
        if (rawPath == "root") return FileRootRef
        val path = sanitizePath(rawPath)
        val bundledRef = BundledRef.parse(path) // is cached, so it's fine to be here
        if (bundledRef != null) return bundledRef
        return fileCache.getEntry(path, fileTimeout, true, generator)
    }

    private fun sanitizePath(str: String): String {
        val str2 = if ('\\' in str) str.replace('\\', '/') else str
        val str3 = if (
            !isWindowsDriveLetterWithoutSlash(str2) &&
            str2.endsWith('/') && !str2.endsWith("://")
        ) str2.substring(0, str2.lastIndex) else str2
        val str4 = if ("/../" in str3 || str3.endsWith("/..") ||
            "/./" in str3 || str3.endsWith("/.")
        ) {
            val parts = str3.split('/').toMutableList()
            var i = 0
            while (i < parts.size) {
                val part = parts[i]
                when (part) {
                    ".." -> {
                        parts.removeAt(i)
                        parts.removeAt(i - 1)
                    }
                    "." -> parts.removeAt(i)
                }
                i++
            }
            parts.joinToString("/")
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
            return WebRef(absolutePath, emptyMap())
        }

        // runtime-only resource
        if (isTemporaryFile(absolutePath)) {
            return resolveTemporaryFile(absolutePath)
        }

        // resource, which is defined by always-existing object
        val static = staticReferences[absolutePath]
        if (static != null) {
            return static
        }

        // real or compressed files
        // check whether it exists -> easy then :)
        if (LastModifiedCache.exists(absolutePath)) {
            return createFileFileRef(absolutePath)
        }

        if (OS.isWindows) {
            assertTrue(isWindowsDriveLetterWithoutSlash(absolutePath) || ":/" in absolutePath) {
                "Invalid file ($absolutePath)"
            }
        }

        // split by /, and check when we need to enter a zip file
        val parts = absolutePath.trim().split('/', '\\')
        val builder = StringBuilder(absolutePath)
        for (i in parts.lastIndex downTo 0) {
            val substr = builder.toString()
            if (i < parts.lastIndex && LastModifiedCache.exists(substr)) {
                // great :), now go into that file
                return appendPath(File(substr), i + 1, parts)
            }
            if (i > 0) {
                var newLength = builder.length - (parts[i].length + 1)
                // keep slash for driver-letter
                if (newLength == 2 && builder[newLength - 1] == ':') newLength++
                builder.setLength(newLength)
            } // else builder no longer needed
        }

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

        // real or compressed files
        // check whether it exists -> easy then :)
        if (LastModifiedCache.exists(absolutePath)) {
            return createFileFileRef(absolutePath)
        }

        return LinkFileReference(absolutePath)
    }

    private fun isWebResource(absolutePath: String): Boolean {
        return absolutePath.startsWith("http://", true) || absolutePath.startsWith("https://", true)
    }

    private fun isTemporaryFile(absolutePath: String): Boolean {
        return absolutePath.startsWith(InnerTmpFile.PREFIX)
    }

    private fun resolveTemporaryFile(absolutePath: String): FileReference {
        val tmp = InnerTmpFile.find(absolutePath)
        if (tmp == null) LOGGER.warn("$absolutePath could not be found, maybe it was created in another session, or GCed")
        return tmp ?: InvalidRef
    }

    fun isWindowsDriveLetterWithoutSlash(absolutePath: String): Boolean {
        return absolutePath.length == 2 && absolutePath[1] == ':' &&
                (absolutePath[0] in 'A'..'Z' || absolutePath[0] in 'a'..'z')
    }

    private fun createFileFileRef(absolutePath: String): FileFileRef {
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
            pe && ne -> "${parent.substring(0, parent.lastIndex)}$name"
            pe || ne -> "$parent$name"
            else -> "$parent/$name"
        }
    }
}