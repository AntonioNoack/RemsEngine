package me.anno.io.zip

import me.anno.Engine
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.appendPath
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import kotlin.io.path.name
import kotlin.streams.toList

@Deprecated("This class isn't ready for use yet")
class InnerZipFileV2(
    absolutePath: String,
    val fileSystem: ZipFileSystem,
    relativePath: String,
    parent: FileReference
) : InnerFolder(absolutePath, relativePath, parent) {

    class ZipFileSystem(private val sourcePath: String) {

        private fun getRoot(): Path {
            return fileSystem.rootDirectories.first()
        }

        private fun createFileSystem(): FileSystem {
            val path = Paths.get(sourcePath)
            return FileSystems.newFileSystem(path, null as ClassLoader?)
        }

        private var fileSystem = createFileSystem()
        private var root: Path = getRoot()

        fun getPath(relativePath: String): Path {
            return synchronized(this) {
                if (relativePath.isEmpty()) root
                else root.resolve(relativePath)
            }
        }

        fun invalidate() {
            synchronized(this) {
                println("Writing all changes")
                fileSystem.close()
                fileSystem = createFileSystem()
                root = getRoot()
            }
        }

        private var isRegistered = false

        fun notifyChangeHappened() {
            synchronized(this) {
                if (isRegistered) return
                isRegistered = true
                Engine.registerForShutdown {
                    fileSystem.close()
                }
            }
        }
    }

    val path get() = fileSystem.getPath(relativePath)

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        var stream: InputStream? = null
        try {
            stream = Files.newInputStream(path)
            callback.ok(stream.useBuffered())
        } catch (e: IOException) {
            callback.err(e)
        } finally {
            if (closeStream) stream?.close()
        }
    }

    override fun outputStream(append: Boolean): OutputStream {

        // ensure parent folder exists
        if ('/' in relativePath) {
            getParent().tryMkdirs()
        }

        val rawStream =
            if (append) Files.newOutputStream(path, StandardOpenOption.APPEND)
            else Files.newOutputStream(path)

        return object : OutputStream() {
            override fun write(b: ByteArray) {
                rawStream.write(b)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                rawStream.write(b, off, len)
            }

            override fun write(b: Int) {
                rawStream.write(b)
            }

            override fun flush() {
                rawStream.flush()
            }

            override fun close() {
                rawStream.close()
                fileSystem.notifyChangeHappened()
            }
        }.buffered()
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        try {
            val bytes = Files.readAllBytes(path)
            callback.ok(bytes)
        } catch (e: IOException) {
            callback.err(e)
        }
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        val bytes1 =
            if (offset == 0 && length == bytes.size) bytes
            else bytes.copyOfRange(offset, offset + length)
        Files.write(path, bytes1)
        fileSystem.notifyChangeHappened()
    }

    override val exists: Boolean
        get() = Files.exists(path)

    override var lastModified: Long
        get() = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis()
        set(value) {
            Files.setLastModifiedTime(path, FileTime.from(value, TimeUnit.MILLISECONDS))
            fileSystem.notifyChangeHappened()
        }

    override val isDirectory: Boolean
        get() = Files.isDirectory(path)

    override fun length(): Long {
        return try {
            Files.size(path)
        } catch (_: IOException) {
            Int.MAX_VALUE.toLong()
        }
    }

    override fun delete(): Boolean {
        return try {
            Files.delete(path)
            true
        } catch (_: IOException) {
            false
        }
    }

    override fun mkdirs(): Boolean {
        try {
            Files.createDirectory(path)
            fileSystem.notifyChangeHappened()
            return true
        } catch (_: IOException) {
            return false
        }
    }

    override fun getChildImpl(name: String): FileReference {
        val child =
            if (!exists || isDirectory) getChildByName(name)
            else super.getChildImpl(name)
        println("$this[$name] -> $child")
        return child
    }

    override fun listChildren(callback: Callback<List<FileReference>>) {
        callback.ok(Files.list(path).toList().map { child ->
            var name = child.name
            if (name.endsWith('/')) name = name.substring(0, name.lastIndex)
            getChildByName(name)
        })
    }

    private fun getChildByName(childName: String): FileReference {
        val absolutePath = appendPath(absolutePath, childName)
        val relativePath = appendPath(relativePath, childName)
        return InnerZipFileV2(absolutePath, fileSystem, relativePath, this)
    }

    companion object {

        fun createZipFile(
            source: FileReference,
            callback: Callback<InnerFolder>
        ) {
            try {
                val fileSystem = ZipFileSystem(source.absolutePath)
                val file = InnerZipFileV2(source.absolutePath, fileSystem, "", source.getParent())
                callback.ok(file)
            } catch (e: IOException) {
                callback.err(e)
            }
        }
    }
}