package me.anno.io.files

import me.anno.cache.IgnoredException
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.Reference.appendPath
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Reference.register
import me.anno.utils.structures.Callback
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import kotlin.concurrent.thread

class FileFileRef(val file: File) : FileReference(beautifyPath(file.absolutePath)) {

    companion object {

        // <= 0 = disabled, > 0 -> tracks all open files for debugging
        private var trackOpenStreamsMillis = 0L

        fun createTempFile(name: String, extension: String): FileReference {
            return FileFileRef(File.createTempFile(name, ".$extension"))
        }

        private fun beautifyPath(path: String): String {
            var p = path.replace('\\', '/')
            while (p.endsWith('/')) p = p.substring(0, p.length - 1)
            return p
        }
    }

    override fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) {
        try {
            callback.ok(inputStreamSync())
        } catch (_: IgnoredException) {
            callback.call(null, null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    override fun inputStreamSync(): InputStream {
        val base = file.inputStream().useBuffered()
        if (trackOpenStreamsMillis < 1) return base
        var closed = false
        val stack = Throwable("$this was not closed!")
        thread {
            Thread.sleep(trackOpenStreamsMillis)
            if (!closed) {
                stack.printStackTrace()
            }
        }
        return object : InputStream() {
            override fun read() = base.read()
            override fun mark(p0: Int) = base.mark(p0)
            override fun markSupported() = base.markSupported()
            override fun read(p0: ByteArray, p1: Int, p2: Int) = base.read(p0, p1, p2)
            override fun read(p0: ByteArray) = base.read(p0)
            override fun close() {
                base.close()
                closed = true
            }
        }
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        try {
            callback.ok(file.readBytes())
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    override fun readBytesSync() = file.readBytes()

    override fun readText(callback: Callback<String>) {
        try {
            callback.ok(file.readText())
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    override fun readTextSync() = file.readText()

    override fun outputStream(append: Boolean): OutputStream {
        val ret = FileOutputStream(file, append).useBuffered()
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

    override fun writeFile(
        file: FileReference,
        progress: (delta: Long, total: Long) -> Unit,
        callback: (Exception?) -> Unit
    ) {
        super.writeFile(file, progress, callback)
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

    override fun listChildren(): List<FileReference> {
        return (if (exists) {
            if (isDirectory) file.listFiles()?.map { getChild(it.name) }
            else zipFileForDirectory?.listChildren()
        } else null) ?: super.listChildren()
    }

    override fun getParent(): FileReference =
        getReference(file.parentFile?.absolutePath).ifUndefined(FileRootRef)

    override fun renameTo(newName: FileReference): Boolean {
        val response = file.renameTo(
            if (newName is FileFileRef) newName.file
            else File(newName.absolutePath)
        )
        if (response) {
            LastModifiedCache.invalidate(this)
            LastModifiedCache.invalidate(newName)
        }
        return response
    }

    override fun getChild(name: String): FileReference {
        return if (!exists || isDirectory) {
            if ('/' in name || '\\' in name) getReference(appendPath(absolutePath, name))
            else register(FileFileRef(File(file, name)))
        } else zipFileForDirectory?.getChild(name) ?: InvalidRef // todo
    }

    override val exists: Boolean
        get() = LastModifiedCache[file, absolutePath].exists

    override val lastModified: Long
        get() = LastModifiedCache[file, absolutePath].lastModified

    override val lastAccessed: Long
        get() = LastModifiedCache[file, absolutePath].lastAccessed

    override val creationTime: Long
        get() = LastModifiedCache[file, absolutePath].creationTime

    override fun toUri(): URI {
        return URI("file:/${absolutePath.replace(" ", "%20")}")
    }

    override val isDirectory: Boolean
        get() = LastModifiedCache[file, absolutePath].isDirectory
}