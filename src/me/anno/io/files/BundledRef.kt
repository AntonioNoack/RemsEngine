package me.anno.io.files

import me.anno.io.BufferedIO.useBuffered
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

// internally in the JAR
class BundledRef(
    private val resName: String, absolute: String = "$prefix$resName",
    override val isDirectory: Boolean
) : FileReference(absolute) {

    constructor(resName: String) : this(resName, "$prefix$resName", false)

    override fun getChild(name: String): FileReference {

        // todo for the most important directories, e.g. asset directories,
        // todo we could add index.txt files or sth like that, where all sub-files are listed

        // todo or we could identify where the zip jar is located, and traverse/index it

        TODO("Not yet implemented")
    }

    override fun inputStream(): InputStream {
        // needs to be the same package
        val stream = BundledRef::class.java.classLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException(absolutePath)
        return stream.useBuffered()
    }

    override fun outputStream(): OutputStream {
        throw IllegalAccessException("Cannot write to internal files")
    }

    override val exists: Boolean = true // mmh...
    override fun length(): Long = Int.MAX_VALUE.toLong() // mmh...

    override fun delete(): Boolean {
        throw IllegalAccessException("Cannot write to internal files")
    }

    override fun mkdirs(): Boolean {
        throw IllegalAccessException("Cannot write to internal files")
    }

    override fun renameTo(newName: FileReference): Boolean {
        throw IllegalAccessException("Cannot write to internal files")
    }

    val parent = lazy {
        // check whether / is in path
        val li = resName.lastIndexOf('/')
        if (li >= 0) {
            val newName = resName.substring(0, li)
            val newPath = absolutePath.substring(0, li + prefix.length)
            BundledRef(newName, newPath, true)
        } else jarAsZip.value
    }

    override fun getParent(): FileReference? {
        return parent.value
    }

    override val lastModified: Long = 0L
    override val lastAccessed: Long = 0L

    override fun toUri(): URI {// mmh...
        return URI(absolutePath)
    }

    companion object {

        fun parse(str: String): FileReference {
            if (!str.startsWith(prefix, true)) throw IllegalArgumentException()
            val zip = jarAsZip.value
            if (zip == null) {
                BundledRef(str.substring(5), str, false)
            } else {
                getReference(zip, str.substring(prefix.length))
            }
            // is directory may be false...
            return BundledRef(str.substring(5), str, false)
        }

        const val prefix = "res://"
        private val jarAsZip = lazy<FileReference?> {
            // todo find this jar file as zip
            null
        }
    }

}