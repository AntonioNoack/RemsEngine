package me.anno.io.files

import me.anno.io.BufferedIO.useBuffered
import java.io.*
import java.net.URI

// internally in the JAR
class BundledRef(
    private val resName: String, absolute: String = "$prefix$resName",
    override val isDirectory: Boolean
) : FileReference(absolute) {

    constructor(resName: String) : this(resName, "$prefix$resName", false)

    // todo for the most important directories, e.g. asset directories,
    //  we could add index.txt files or sth like that, where all sub-files are listed
    // done for desktop: or we could identify where the zip jar is located, and traverse/index it

    override fun getChild(name: String) = getReference(zipFileForDirectory, name)

    override fun inputStream(): InputStream {
        // needs to be the same package
        val stream = BundledRef::class.java.classLoader.getResourceAsStream(resName)
            ?: throw FileNotFoundException(absolutePath)
        return stream.useBuffered()
    }

    override fun outputStream(): OutputStream {
        throw IllegalAccessException("Cannot write to internal files")
    }

    override val exists: Boolean = true // mmh...
    override fun length(): Long {
        var length = 0L
        inputStream().use {
            when (it) {
                is ByteArrayInputStream ->
                    length = it.available().toLong()
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
        } else jarAsZip
    }

    override fun getParent() = cachedParent

    override val lastModified: Long = 0L
    override val lastAccessed: Long = 0L

    override fun toUri(): URI {// mmh...
        return URI(absolutePath)
    }

    companion object {

        fun parse(str: String): FileReference {
            if (!str.startsWith(prefix, true)) throw IllegalArgumentException()
            val zip = jarAsZip
            if (zip == InvalidRef) {
                BundledRef(str.substring(prefix.length), str, false)
            } else {
                getReference(zip, str.substring(prefix.length))
            }
            // is directory may be false...
            return BundledRef(str.substring(prefix.length), str, false)
        }

        const val prefix = "res://"
        private val jarAsZip by lazy {
            // find this jar file as zip
            try {
                getReference(
                    File(
                        Companion::class.java
                            .protectionDomain
                            .codeSource
                            .location
                            .toURI()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
                InvalidRef
            }
        }
    }

}