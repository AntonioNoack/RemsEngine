package me.anno.io.files

import me.anno.io.VoidOutputStream
import me.anno.io.files.Reference.appendPath
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * files, that are included with the .jar file
 * */
class BundledRef private constructor(
    private val resName: String, absolute: String,
    private val parentRef: FileReference
) : FileReference(absolute) {

    override fun getChildImpl(name: String): FileReference {
        val fromZip = zipFileForDirectory?.getChildImpl(name)
        if (fromZip != null && fromZip != InvalidRef) return fromZip
        val fullPath = appendPath(absolutePath, name)
        return synchronized(cache) {
            cache.getOrPut(fullPath) {
                BundledRef(appendPath(resName, name), fullPath, this)
            }
        }
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        val stream = inputStreamNullable()
        val error = if (stream == null) FileNotFoundException(absolutePath) else null
        callback.call(stream, error)
    }

    override fun inputStreamSync(): InputStream {
        return inputStreamNullable() ?: throw FileNotFoundException(absolutePath)
    }

    private fun inputStreamNullable(): InputStream? {
        return javaClass.classLoader.getResourceAsStream(resName)
    }

    // not ideal, if we just want to know if it exists, but not also its length
    override val exists get() = cachedLength != null

    private val cachedLength by lazy {
        // the length is what we can skip
        inputStreamNullable()?.skip(Int.MAX_VALUE.toLong())
    }

    override fun length(): Long = cachedLength ?: 0L

    override fun outputStream(append: Boolean): OutputStream {
        LOGGER.warn("Cannot write to internal files")
        return VoidOutputStream
    }

    // these operations are all impossible; should they print warnings?
    override fun delete(): Boolean = false
    override fun mkdirs(): Boolean = false
    override fun renameTo(newName: FileReference): Boolean = false

    override fun getParent(): FileReference = parentRef

    override val isDirectory: Boolean get() = false
    override val lastModified get(): Long = 0L
    override val lastAccessed get(): Long = 0L
    override val creationTime get(): Long = 0L

    companion object {

        private val LOGGER = LogManager.getLogger(BundledRef::class)

        const val PREFIX = "res://"
        val origin = BundledRef("", PREFIX, FileRootRef)

        // the instances are relatively small, and there's a finite amount of them
        //  -> let's just cache them all
        private val cache = HashMap<String, FileReference>()

        fun parse(absolutePath: String): FileReference? {
            return if (absolutePath.startsWith(PREFIX, true)) {
                synchronized(cache) { cache[absolutePath] }
                    ?: create(absolutePath)
            } else null
        }

        private fun create(absolutePath: String): FileReference? {
            return origin.getChildUnsafe(absolutePath.substring(PREFIX.length))
        }
    }
}