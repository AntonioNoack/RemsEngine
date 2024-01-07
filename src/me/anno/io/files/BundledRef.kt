package me.anno.io.files

import me.anno.Build
import me.anno.io.BufferedIO.useBuffered
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.URI

// internally in the JAR
class BundledRef(
    private val resName: String, absolute: String = "$prefix$resName",
    override val isDirectory: Boolean
) : FileReference(absolute) {

    constructor(resName: String) : this(resName, "$prefix$resName", false)

    override fun getChild(name: String): FileReference {
        val zfd = zipFileForDirectory
        return zfd?.getChild(name) ?: parse("$absolutePath/$name")
    }

    override fun inputStream(lengthLimit: Long, callback: (it: InputStream?, exc: Exception?) -> Unit) {
        // needs to be the same package
        val stream = javaClass.classLoader.getResourceAsStream(resName)
        callback(stream?.useBuffered(), if (stream == null) FileNotFoundException(absolutePath) else null)
    }

    override fun outputStream(append: Boolean): OutputStream {
        throw IllegalAccessException("Cannot write to internal files")
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
                            println(test)
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
        throw IllegalAccessException("Cannot write to internal files")
    }

    override fun mkdirs(): Boolean {
        throw IllegalAccessException("Cannot write to internal files")
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw IllegalAccessException("Cannot write to internal files")
    }

    private val cachedParent by lazy {
        // check whether / is in path
        val li = resName.lastIndexOf('/')
        if (li >= 0) {
            val newName = resName.substring(0, li)
            val newPath = absolutePath.substring(0, li + prefix.length)
            BundledRef(newName, newPath, true)
        } else jarAsZip2
    }

    override fun getParent() = cachedParent

    override val lastModified get(): Long = 0L
    override val lastAccessed get(): Long = 0L
    override val creationTime get(): Long = 0L

    override fun toUri(): URI {// mmh...
        return URI(absolutePath)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(BundledRef::class)

        fun parse(fullPath: String): FileReference {

            if (!fullPath.startsWith(prefix, true)) throw IllegalArgumentException()
            val resName = fullPath.substring(prefix.length)

            // if asset folder is declared, and the asset is located there, use it for realtime-reloading
            val child = Build.assetsFolder.getChild(resName)
            if (child.exists) return child

            // surprisingly, this caused issues with language files
            /*if (mainName.indexOf('/') >= 0 && jarAsZip.isNotEmpty()) {
                // find whether any prefix is good enough, like the search for the files
                val index = jarAsZip
                val parts = str.lowercase().split('/', '\\')

                // binary search? let's do linear first
                for (i in parts.lastIndex downTo 0) {
                    val substr = parts.subList(0, i).joinToString("/")
                    if (substr in index) {
                        // great :), now go into that file
                        val baseFile = BundledRef(substr, prefix + substr, false)
                        return appendPath(baseFile, i, parts)
                    }
                }
            }*/
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
                        fullPath.substring(0, prefix.length + lastSlash)
                    )?.getChild(resName.substring(lastSlash + 1))
                } else null
            } catch (e: StackOverflowError) {
                LOGGER.warn("StackOverflow happened for '$resName'/'$fullPath'")
                return null
            }
        }

        const val prefix = "res://"

        // todo when we ship the game, we can just pack this data into some kind of txt file
        // todo required data: HashSet<FileNameLowerCase>
        private val jarAsZip3 by lazy {
            try {
                val uri = Companion::class.java
                    .protectionDomain
                    ?.codeSource
                    ?.location
                    ?.toURI()
                if (uri != null) File(uri) else null
            } catch (e: NullPointerException) {
                e.printStackTrace()
                null
            }
        }

        private val jarAsZip2 by lazy { getReference(jarAsZip3) }
    }
}