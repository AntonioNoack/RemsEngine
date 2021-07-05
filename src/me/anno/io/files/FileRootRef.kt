package me.anno.io.files

import java.io.File
import java.io.IOException
import java.net.URI
import kotlin.system.exitProcess

object FileRootRef : FileReference("null") {

    override fun inputStream() = throw IOException()

    override fun outputStream() = throw IOException()

    override fun length() = 0L

    // whatever you're trying to do, it's bad. Really bad.
    override fun deleteRecursively(): Boolean {
        System.err.println("WTF are you trying to do? This call would have deleted your whole computer!")
        exitProcess(-1)
    }

    override fun deleteOnExit() {}

    override fun delete(): Boolean = false

    override fun mkdirs(): Boolean = true

    override fun listChildren() = File.listRoots().map { getReference(it) }

    override fun getChild(name: String): FileReference? {
        return listChildren().firstOrNull { it.name == name }
    }

    override fun getParent() = null

    override fun renameTo(newName: FileReference): Boolean {
        return false
    }

    override val isDirectory: Boolean
        get() = true

    override val exists: Boolean
        get() = true

    override val lastModified: Long
        get() = 0L

    override val lastAccessed: Long
        get() = 0L

    override fun toUri(): URI {
        return URI("file://")
    }

}