package me.anno.io.files

import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class FileFileRef(val file: File) : FileReference(beautifyPath(file.absolutePath)) {

    companion object {

        fun beautifyPath(path: String): String {
            var p = path.replace('\\', '/')
            while (p.endsWith('/')) p = p.substring(0, p.length - 1)
            return p
        }

    }

    override fun inputStream() = file.inputStream().buffered()

    override fun outputStream() = file.outputStream().buffered()

    override fun length() = file.length()

    override fun deleteRecursively(): Boolean = file.deleteRecursively()

    override fun deleteOnExit() {
        file.deleteOnExit()
    }

    override fun delete(): Boolean = file.delete()

    override fun mkdirs(): Boolean = file.mkdirs()

    override fun listChildren(): List<FileReference>? {
        return (if (exists) {
            if (isDirectory) {
                file.listFiles()?.map { FileFileRef(it) }
            } else {
                zipFileForDirectory?.listChildren()
            }
        } else null) ?: super.listChildren()
    }

    override fun getParent() = getReference(file.parentFile)

    override fun renameTo(newName: FileReference) = file.renameTo(File(newName.absolutePath))

    override fun getChild(name: String): FileReference {
        return if (!exists || isDirectory) {
            if ('/' in name || '\\' in name) {
                getReference(this, name)
            } else {
                FileFileRef(File(file, name))
            }
        } else {
            getReference(zipFileForDirectory, name)
        }
    }

    override val exists: Boolean
        get() = file.exists()

    override val lastModified: Long
        get() = file.lastModified()

    override val lastAccessed: Long
        get() {
            return try {
                Files.readAttributes(
                    file.toPath(),
                    BasicFileAttributes::class.java
                )?.lastAccessTime()?.toMillis() ?: 0L
            } catch (ignored: IOException) {
                0L
            }
        }

    override fun toUri(): URI {
        return URI("file://$absolutePath")
    }

    override val isDirectory: Boolean
        get() = file.isDirectory

}