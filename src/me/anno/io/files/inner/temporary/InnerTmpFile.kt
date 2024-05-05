package me.anno.io.files.inner.temporary

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.utils.types.Strings.isBlank2
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
        if (prefix.isBlank2()) "tmp://$uuid.$ext"
        else "tmp://$prefix.$uuid.$ext", uuid
    )

    override fun toLocalPath(workspace: FileReference) = absolutePath

    companion object { // only works if extension does not contain dots
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
        fun find(str: String): InnerTmpFile? {
            // prefix.uuid.ext or prefix.ext
            LOGGER.debug("Parsing $str")
            val endOfExt = str.lastIndexOf('.')
            if (endOfExt < 3) return null
            var prevOfExt = str.lastIndexOf('.', endOfExt - 1)
            if (prevOfExt < 0) prevOfExt = str.lastIndexOf('/', endOfExt - 2)
            LOGGER.debug("Reading index from ${str.substring(prevOfExt + 1, endOfExt)}")
            val uuid = str.substring(prevOfExt + 1, endOfExt).toIntOrNull() ?: return null
            LOGGER.debug("Getting file with index $uuid")
            val reference = files.getOrNull(uuid) ?: return null
            val candidate = reference.get()
            if (candidate != null) {
                if (candidate.absolutePath == str) return candidate
                LOGGER.warn("InnerTmpFile mismatch: searched for '$str', but found '$candidate'")
                return null
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