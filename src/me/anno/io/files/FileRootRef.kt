package me.anno.io.files

import me.anno.io.files.Reference.getReference
import me.anno.utils.structures.Callback
import java.io.File
import java.io.IOException
import java.io.InputStream

object FileRootRef : FileReference("root") {

    override fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) =
        throw IOException()

    override fun outputStream(append: Boolean) = throw IOException()

    override fun length() = 0L

    /**
     * Whatever you're trying to do, it's horrendous;
     * This function must not be called.
     * */
    override fun deleteRecursively(): Boolean {
        throw Error("WTF are you trying to do? This call would have deleted your whole computer!")
    }

    override fun deleteOnExit() {}

    override fun delete(): Boolean = false

    override fun mkdirs(): Boolean = true

    override fun listChildren(): List<FileReference> = File.listRoots().map { getReference(it.absolutePath) }

    override fun getChild(name: String): FileReference {
        return listChildren().firstOrNull { it.name == name } ?: InvalidRef
    }

    override fun getParent(): FileReference = InvalidRef

    override fun renameTo(newName: FileReference): Boolean {
        return false
    }

    override val isDirectory: Boolean get() = true
    override val exists: Boolean get() = true
    override val lastModified: Long get() = 0L
    override val lastAccessed: Long get() = 0L
    override val creationTime: Long get() = 0L
}