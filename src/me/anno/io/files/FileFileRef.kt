package me.anno.io.files

import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.cache.IgnoredException
import me.anno.utils.Threads
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.VoidOutputStream
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FileFileRef(val file: File) : FileReference(beautifyPath(file.absolutePath)) {

    companion object {

        private val LOGGER = LogManager.getLogger(FileFileRef::class)

        fun createTempFile(name: String, extension: String): FileReference {
            return FileFileRef(File.createTempFile(name.padEnd(5, '-'), if (extension.isEmpty()) "" else ".$extension"))
        }

        fun createTempFolder(name: String): FileReference {
            val file = createTempFile(name, "tmp")
            file.delete()
            file.tryMkdirs()
            return file
        }

        private fun beautifyPath(path: String): String {
            var p = path.replace('\\', '/')
            while (p.endsWith('/')) p = p.substring(0, p.length - 1)
            return p
        }
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        runOnNonGFXThread(absolutePath) {
            var stream: InputStream? = null
            try {
                stream = file.inputStream().useBuffered()
                callback.ok(stream)
            } catch (_: IgnoredException) {
                callback.call(null, null)
            } catch (e: Throwable) {
                callback.err(IOException("Failure reading '$this'", e))
            } finally {
                if (closeStream) {
                    stream?.close()
                }
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

    override fun outputStream(append: Boolean): OutputStream {
        try {
            val ret = FileOutputStream(file, append).useBuffered()
            // when writing is finished, this should be called again
            LastModifiedCache.invalidate(file)
            return ret
        } catch (_: Throwable) {
            LOGGER.warn("Failed creating outputStream to '$this'")
            return VoidOutputStream
        }
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        try {
            FileOutputStream(file, false).use { stream ->
                stream.write(bytes, offset, length)
            }
            LastModifiedCache.invalidate(file)
        } catch (e: Throwable) {
            LOGGER.warn("Failed writing to '$this'", e)
        }
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

    override fun listChildren(callback: Callback<List<FileReference>>) {
        Threads.runTaskThread("$absolutePath.listChildren") { // can be extremely slow
            if (exists && isDirectory) {
                try {
                    val answer = file.listFiles()?.map { getChild(it.name) }
                    if (answer != null) return@runTaskThread callback.ok(answer)
                } catch (_: Throwable) {
                    // failed reading :/
                }
            }
            super.listChildren(callback)
        }
    }

    override fun renameTo(newName: FileReference): Boolean {
        val newName = newName.resolved()
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
            FileFileRef(File(file, name))
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