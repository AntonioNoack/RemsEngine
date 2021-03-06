package me.anno.io.files

import me.anno.cache.instances.LastModifiedCache
import me.anno.io.Streams.copy
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset

class FileFileRef(val file: File) : FileReference(beautifyPath(file.absolutePath)) {

    companion object {

        fun createTempFile(name: String, extension: String): FileReference {
            return getReference(File.createTempFile(name, extension))
        }

        fun beautifyPath(path: String): String {
            var p = path.replace('\\', '/')
            while (p.endsWith('/')) p = p.substring(0, p.length - 1)
            return p
        }

        fun copyHierarchy(src: FileReference, dst: File) {
            if (src.isDirectory) {
                dst.mkdirs()
                for (child in src.listChildren() ?: emptyList()) {
                    copyHierarchy(child, File(dst, child.name))
                }
            } else {
                src.inputStream().copy(dst.outputStream())
            }
        }

    }

    override fun toFile(): File = file

    override fun inputStream() = file.inputStream().buffered()

    override fun outputStream(): OutputStream {
        val ret = file.outputStream().buffered()
        // when writing is finished, this should be called again
        LastModifiedCache.invalidate(file)
        return ret
    }

    override fun writeText(text: String) {
        file.writeText(text)
        LastModifiedCache.invalidate(file)
    }

    override fun writeBytes(bytes: ByteArray) {
        file.writeBytes(bytes)
        LastModifiedCache.invalidate(file)
    }

    override fun writeText(text: String, charset: Charset) {
        file.writeText(text, charset)
        LastModifiedCache.invalidate(file)
    }

    override fun length() = LastModifiedCache[file, absolutePath].length

    override fun deleteRecursively(): Boolean {
        // todo also invalidate children
        val success = file.deleteRecursively()
        if (success) LastModifiedCache.invalidate(absolutePath)
        return success
    }

    override fun deleteOnExit() {
        file.deleteOnExit()
    }

    override fun delete(): Boolean {
        val success = file.delete()
        if (success) LastModifiedCache.invalidate(absolutePath)
        return success
    }

    override fun mkdirs(): Boolean {
        val success = file.mkdirs()
        if (success) LastModifiedCache.invalidate(absolutePath)
        return success
    }

    override fun hasChildren(): Boolean {
        return file.listFiles()?.isNotEmpty() == true
    }

    override fun listChildren(): List<FileReference>? {
        return (if (exists) {
            if (isDirectory) file.listFiles()?.map { getReference(it) }
            else zipFileForDirectory?.listChildren()
        } else null) ?: super.listChildren()
    }

    override fun getParent() = getReference(file.parentFile).nullIfUndefined() ?: FileRootRef

    override fun renameTo(newName: FileReference) = file.renameTo(File(newName.absolutePath))

    override fun getChild(name: String): FileReference {
        return if (!exists || isDirectory) {
            if ('/' in name || '\\' in name) getReference(this, name)
            else register(FileFileRef(File(file, name)))
        } else getReference(zipFileForDirectory, name)
    }

    override val exists: Boolean
        get() = LastModifiedCache[file, absolutePath].exists

    override val lastModified: Long
        get() = LastModifiedCache[file, absolutePath].lastModified

    override val lastAccessed: Long
        get() = LastModifiedCache[file, absolutePath].lastAccessed

    override fun toUri(): URI {
        return URI("file:/${absolutePath.replace(" ", "%20")}")
    }

    override val isDirectory: Boolean
        get() = LastModifiedCache[file, absolutePath].isDirectory

}