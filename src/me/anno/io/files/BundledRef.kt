package me.anno.io.files

import me.anno.Build
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.Reference.appendPath
import me.anno.utils.structures.Callback
import org.apache.logging.log4j.LogManager
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * files, that are included with the .jar file
 * */
class BundledRef(
    private val resName: String, absolute: String = "$PREFIX$resName",
    override val isDirectory: Boolean
) : FileReference(absolute) {

    constructor(resName: String) : this(resName, "$PREFIX$resName", false)

    override fun getChild(name: String): FileReference {
        val zfd = zipFileForDirectory
        return zfd?.getChild(name) ?: parse(appendPath(absolutePath, name))
    }

    override fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) {
        // needs to be the same package
        val stream = javaClass.classLoader.getResourceAsStream(resName)
        callback.call(stream?.useBuffered(), if (stream == null) FileNotFoundException(absolutePath) else null)
    }

    override fun outputStream(append: Boolean): OutputStream {
        throw IOException("Cannot write to internal files")
    }

    override val exists by lazy {
        javaClass.classLoader.getResourceAsStream(resName) != null
    }

    override fun length(): Long {
        var length = 0L
        try {
            inputStreamSync().use {
                when (it) {
                    is ByteArrayInputStream ->
                        length = it.available().toLong()

                    is BufferedInputStream -> {
                        val buffer = ByteArray(2048)
                        while (true) {
                            val len = it.read(buffer)
                            if (len < 0) break
                            length += len
                        }
                    }

                    else -> {
                        // todo this doesn't work :/
                        // https://stackoverflow.com/questions/34360826/get-the-size-of-a-resource might work, when we find the correct jar
                        var test = 1L shl 16 // 65k .. as large as needed
                        while (test > 0L) {
                            val skipped = it.skip(test)
                            if (skipped <= 0) break
                            length += skipped
                            test = test shl 1
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            length = 65536
        }
        return length
    }

    override fun delete(): Boolean {
        throw IOException("Cannot write to internal files")
    }

    override fun mkdirs(): Boolean {
        throw IOException("Cannot write to internal files")
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw IOException("Cannot write to internal files")
    }

    private val cachedParent by lazy {
        // check whether / is in path -> whether it's a child
        val li = resName.lastIndexOf('/')
        if (li >= 0) {
            val newName = resName.substring(0, li)
            val newPath = absolutePath.substring(0, li + PREFIX.length)
            BundledRef(newName, newPath, true)
        } else FileRootRef // not truly correct, but probably better than InvalidRef
    }

    override fun getParent(): FileReference = cachedParent

    override val lastModified get(): Long = 0L
    override val lastAccessed get(): Long = 0L
    override val creationTime get(): Long = 0L

    companion object {

        private val LOGGER = LogManager.getLogger(BundledRef::class)

        fun parse(fullPath: String): FileReference {

            if (!fullPath.startsWith(PREFIX, true)) throw IllegalArgumentException()
            val resName = fullPath.substring(PREFIX.length)

            // if asset folder is declared, and the asset is located there, use it for realtime-reloading
            val child = Build.assetsFolder.getChild(resName)
            if (child.exists) return child

            // is directory may be false...
            return findExistingReference(resName, fullPath) ?: BundledRef(resName, fullPath, false)
        }

        private fun findExistingReference(resName: String, fullPath: String): FileReference? {
            try {
                val ref = BundledRef(resName, fullPath, false)
                if (ref.exists) return ref
                val lastSlash = resName.lastIndexOf('/')
                val hasSlash = lastSlash in 0 until resName.lastIndex
                return if (hasSlash) {
                    findExistingReference(
                        resName.substring(0, lastSlash),
                        fullPath.substring(0, PREFIX.length + lastSlash)
                    )?.getChild(resName.substring(lastSlash + 1))
                } else null
            } catch (e: StackOverflowError) {
                LOGGER.warn("StackOverflow happened for '$resName'/'$fullPath'")
                return null
            }
        }

        const val PREFIX = "res://"
    }
}