package me.anno.io.files

import me.anno.Time
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.maths.Maths
import me.anno.utils.InternalAPI
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
    fun getReference(str: String?): FileReference {
        // invalid
        if (str == null || str.isBlank2()) return InvalidRef
        // root
        if (str == "root") return FileRootRef
        val str2 = if ('\\' in str) str.replace('\\', '/') else str
        val data = fileCache.getEntry(str2, fileTimeout, false) {
            createReference(it)
        }
        return data ?: createReference(str)
    }

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
    fun getReferenceAsync(str: String?): FileReference? {
        // invalid
        if (str == null || str.isBlank2()) return InvalidRef
        // root
        if (str == "root") return FileRootRef
        val str2 = str.replace('\\', '/')
        val bundledRef = BundledRef.parse(str2) // is cached, so it's fine to be here
        if (bundledRef != null) return bundledRef
        return fileCache.getEntry(str2, fileTimeout, true) {
            createReference(it)
        }
    }

    @JvmStatic
    private fun createReference(str: String): FileReference {

        // internal resource
        val bundledRef = BundledRef.parse(str)
        if (bundledRef != null) return bundledRef

        // web resource
        if (str.startsWith("http://", true) || str.startsWith("https://", true)) {
            return WebRef(str, emptyMap())
        }

        // runtime-only resource
        if (str.startsWith("tmp://")) {
            val tmp = InnerTmpFile.find(str)
            if (tmp == null) LOGGER.warn("$str could not be found, maybe it was created in another session, or GCed")
            return tmp ?: InvalidRef
        }

        // resource, which is defined by always-existing object
        val static = staticReferences[str]
        if (static != null) {
            return static
        }

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
    private fun appendPath(ref0: FileReference, i: Int, parts: List<String>): FileReference {
        var ref = ref0
        for (j in i until parts.size) {
            ref = ref.getChild(parts[j])
            if (ref == InvalidRef) return ref
        }
        return ref
    }

    @JvmStatic
    private fun appendPath(fileI: File, i: Int, parts: List<String>): FileReference {
        return appendPath(FileFileRef(fileI), i, parts)
    }

    @JvmStatic
    fun appendPath(parent: String, name: String): String {
        return if (parent.isBlank2()) name
        else if (parent.endsWith("/") || name.startsWith("/")) "$parent$name"
        else "$parent/$name"
    }
}