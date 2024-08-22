package me.anno.io.files

import me.anno.io.files.Reference.appendPath
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.Callback
import java.io.FileNotFoundException
import java.io.IOException
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
        val zfd = zipFileForDirectory
        if (zfd != null) return zfd.getChild(name)
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
        throw IOException("Cannot write to internal files")
    }

    // these operations are all impossible
    override fun delete(): Boolean = false
    override fun mkdirs(): Boolean = false
    override fun renameTo(newName: FileReference): Boolean = false

    override fun getParent(): FileReference = parentRef

    override val isDirectory: Boolean get() = false
    override val lastModified get(): Long = 0L
    override val lastAccessed get(): Long = 0L
    override val creationTime get(): Long = 0L

    companion object {

        const val PREFIX = "res://"
        val origin = BundledRef("", PREFIX, FileRootRef)

        // the instances are relatively small, and there's a finite amount of them
        //  -> let's just cache them all
        private val cache = HashMap<String, FileReference>()

        fun parse(fullPath: String): FileReference {
            assertTrue(fullPath.startsWith(PREFIX, true))
            return synchronized(cache) { cache[fullPath] }
                ?: origin.getChild(fullPath.substring(PREFIX.length))
        }
    }
}