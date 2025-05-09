package me.anno.io.files.inner.temporary

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.toInt
import org.apache.logging.log4j.LogManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

abstract class InnerTmpFile private constructor(name: String, val uuid: Int) :
    InnerFile(name, name, false, InvalidRef) {

    init {
        if (printTmpFiles) LOGGER.debug("Registered $name")
        synchronized(Companion) {
            val ref = WeakReference(this)
            while (files.size <= uuid) {
                files.add(ref)
            }
            files[uuid] = ref
        }
    }

    constructor(ext: String) : this("", ext)
    constructor(prefix: String, ext: String, uuid: Int = id.getAndIncrement()) : this(
        if (prefix.isBlank2()) "$PREFIX$uuid.$ext"
        else "$PREFIX$prefix.$uuid.$ext", uuid
    )

    override fun toLocalPath(workspace: FileReference) = absolutePath

    companion object { // only works if extension does not contain dots

        const val PREFIX = "tmp://"

        @JvmField
        var printTmpFiles = false

        @JvmStatic
        private val LOGGER = LogManager.getLogger(InnerTmpFile::class)

        @JvmStatic
        private val files = ArrayList<WeakReference<InnerTmpFile>>()

        @JvmStatic
        val prefabFiles = HashMap<String, ArrayList<WeakReference<InnerTmpPrefabFile>>>()

        @JvmStatic
        private val id = AtomicInteger()

        @JvmStatic
        fun find(str: String): FileReference? {
            if (!str.startsWith(PREFIX)) return null
            // prefix.uuid.ext or prefix.ext
            val debug = LOGGER.isDebugEnabled()
            if (debug) LOGGER.debug("Parsing $str")

            val i0 = str.indexOf2('/', PREFIX.length)
            val endOfExt = str.lastIndexOf('.', i0)
            if (endOfExt < 0) return null

            var prevOfExt = str.lastIndexOf('.', endOfExt - 1)
            if (prevOfExt < 0) prevOfExt = PREFIX.length - 1

            if (debug) LOGGER.debug("Reading index from ${str.substring(prevOfExt + 1, endOfExt)}")

            // check UUID validity
            if ((prevOfExt + 1 >= endOfExt)) return null
            for (i in prevOfExt + 1 until endOfExt) {
                if (str[i] !in '0'..'9') return null
            }

            val uuid = (str as CharSequence).toInt(prevOfExt + 1, endOfExt)
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Getting file with index $uuid")

            val reference = files.getOrNull(uuid) ?: return null
            val candidate = reference.get()
            if (candidate != null) {
                var result: FileReference = candidate
                if (i0 + 1 < str.length) {
                    val suffix = str.substring(i0 + 1)
                    result = result.getChildUnsafe(suffix)
                }
                return result
            } else {
                LOGGER.warn("InnerTmpFile #$uuid was already GCed, '$str'!")
                return null
            }
        }

        fun findPrefabs(type: String): List<FileReference> {
            return prefabFiles[type]?.mapNotNull { it.get() } ?: emptyList()
        }
    }
}