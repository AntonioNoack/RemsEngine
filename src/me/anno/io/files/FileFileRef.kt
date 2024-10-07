package me.anno.io.files

import me.anno.cache.IgnoredException
import me.anno.gpu.GFX
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Reference.register
import me.anno.utils.async.Callback
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class FileFileRef(val file: File) : FileReference(beautifyPath(file.absolutePath)) {

    companion object {

        // <= 0 = disabled, > 0 -> tracks all open files for debugging
        private var trackOpenStreamsMillis = 0L

        fun createTempFile(name: String, extension: String): FileReference {
            return FileFileRef(File.createTempFile(name.padEnd(5, '-'), if (extension.isEmpty()) "" else ".$extension"))
        }

        private fun beautifyPath(path: String): String {
            var p = path.replace('\\', '/')
            while (p.endsWith('/')) p = p.substring(0, p.length - 1)
            return p
        }
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        if (GFX.isGFXThread()) {
            thread(name = "inputStream($absolutePath)") {
                inputStream(lengthLimit, closeStream, callback)
            }
        } else {
            var stream: InputStream? = null
            try {
                stream = inputStreamSync()
                callback.ok(stream)
            } catch (_: IgnoredException) {
                callback.call(null, null)
            } catch (e: Exception) {
                callback.err(Exception("Failure reading '$this'", e))
            } finally {
                if (closeStream) {
                    stream?.close()
                }
            }
        }
    }

    override fun inputStreamSync(): InputStream {
        val base = file.inputStream().useBuffered()
        if (trackOpenStreamsMillis < 1) return base
        var closed = false
        val stack = Throwable("$this was not closed!")
        thread(name = "inputStreamSync($absolutePath)") {
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
            override fun skip(n: Long): Long = base.skip(n)
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

    override fun outputStream(append: Boolean): OutputStream {
        val ret = FileOutputStream(file, append).useBuffered()
        // when writing is finished, this should be called again
        LastModifiedCache.invalidate(file)
        return ret
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        FileOutputStream(file, false).use { stream ->
            stream.write(bytes, offset, length)
        }
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

    override fun deleteOnExit() {
        file.deleteOnExit()
    }

    override fun delete(): Boolean {
        val success = file.exists() && file.deleteRecursively()
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

    override fun getChildImpl(name: String): FileReference {
        return if (!exists || isDirectory) {
            register(FileFileRef(File(file, name)))
        } else zipFileForDirectory?.getChildImpl(name) ?: InvalidRef
    }

    override val exists: Boolean
        get() = LastModifiedCache[file, absolutePath].exists

    override val lastModified: Long
        get() = LastModifiedCache[file, absolutePath].lastModified

    override val lastAccessed: Long
        get() = LastModifiedCache[file, absolutePath].lastAccessed

    override val creationTime: Long
        get() = LastModifiedCache[file, absolutePath].creationTime

    override val isDirectory: Boolean
        get() = LastModifiedCache[file, absolutePath].isDirectory
}