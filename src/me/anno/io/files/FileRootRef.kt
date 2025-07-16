package me.anno.io.files

import me.anno.io.VoidOutputStream
import me.anno.io.binary.ByteArrayIO.readLE16
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS
import me.anno.utils.Threads
import me.anno.utils.async.Callback
import me.anno.utils.types.Strings.isNotBlank2
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

object FileRootRef : FileReference("root") {

    private val LOGGER = LogManager.getLogger(FileRootRef::class)

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.err(IOException("Cannot open root as stream"))
    }

    override fun outputStream(append: Boolean): OutputStream {
        LOGGER.warn("Cannot write to FileRootRef")
        return VoidOutputStream
    }

    override fun length() = 0L

    /**
     * Whatever you're trying to do, it's horrendous;
     * This function must not be called.
     * */
    override fun delete(): Boolean {
        throw Error("WTF are you trying to do? This call would have deleted your whole computer!")
    }

    override fun deleteOnExit() {
        delete()
    }

    override fun mkdirs(): Boolean = true

    override fun listChildren(callback: Callback<List<FileReference>>) {
        Threads.start("$absolutePath.listChildren") { // can be extremely slow
            var results = File.listRoots().map { getReference(it.absolutePath) }
            if (OS.isWindows) {
                @Suppress("SuspiciousCollectionReassignment")
                results += findWSLInstances()
            }
            callback.ok(results)
        }
    }

    override fun getChildImpl(name: String): FileReference {
        val file = File.listRoots().firstOrNull { it.name == name } ?: return InvalidRef
        return getReference(file.absolutePath)
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

    /**
     * finds Windows Subsystem for Linux instances
     * */
    fun findWSLInstances(): List<FileReference> {

        val process = Runtime.getRuntime().exec("wsl.exe -l -q")
        val bytes = process.inputStream.readBytes()
        val chars = CharArray(bytes.size shr 1) {
            bytes.readLE16(it * 2).toChar()
        }

        val wslInstanceNames = String(chars).split('\n')
            .filter { it.isNotBlank2() }.map { it.trim() }

        val prefix = "//wsl.localhost/"
        return wslInstanceNames.map { wslInstanceName ->
            getReference("$prefix$wslInstanceName")
        }
    }
}