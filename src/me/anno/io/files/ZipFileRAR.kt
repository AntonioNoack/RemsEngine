package me.anno.io.files

import com.github.junrar.Archive
import com.github.junrar.Volume
import com.github.junrar.exception.RarException
import com.github.junrar.io.IReadOnlyAccess
import com.github.junrar.rarfile.FileHeader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.naming.OperationNotSupportedException

class ZipFileRAR(
    absolutePath: String,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun getInputStream(): InputStream {
        throw OperationNotSupportedException()
    }

    class ZipReadOnlyAccess(val bytes: ByteArray) : IReadOnlyAccess {

        var pos = 0
        override fun setPosition(p0: Long) {
            pos = p0.toInt()
        }

        override fun getPosition(): Long = pos.toLong()
        override fun read(): Int {
            return if (pos < bytes.size) bytes[pos++].toInt().and(255) else -1
        }

        override fun read(p0: ByteArray?, p1: Int, p2: Int): Int {
            p0!!
            if (pos >= bytes.size) return 0
            if (pos + p2 >= bytes.size) {
                // how much is available
                return read(p0, p1, bytes.size - pos)
            }
            for (i in 0 until p2) {
                p0[i + p1] = bytes[pos + i]
            }
            return p2
        }

        override fun readFully(p0: ByteArray?, p1: Int): Int {
            return read(p0, p1, p0!!.size - p1)
        }

        override fun close() {}
    }

    class ZipVolume(val a: Archive, val file: FileReference) : Volume {
        val bytes = lazy { file.readBytes() }
        override fun getReadOnlyAccess(): IReadOnlyAccess {
            return ZipReadOnlyAccess(bytes.value)
        }

        override fun getLength(): Long = file.length()
        override fun getArchive(): Archive = a
    }

    companion object {

        fun fileFromStreamRAR(file: FileReference): Archive {
            return if (file is FileFileRef) {
                Archive(file.file)
            } else {
                // mmmh, probably for loading a set of files
                // which joined result in a large file
                Archive { a, v ->
                    if (v == null) {
                        ZipVolume(a, file)
                    } else TODO()
                }
            }
        }

        fun createZipRegistryRAR(
            zipFileLocation: FileReference,
            getStream: () -> Archive
        ): ZipFileRAR {
            val registry = HashMap<String, ZipFileRAR>()
            val file = ZipFileRAR(
                zipFileLocation.absolutePath, "", true,
                zipFileLocation.getParent() ?: zipFileLocation
            )
            registry[file.relativePath] = file
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                if (zis.isEncrypted) throw IOException("RAR is encrypted")
                while (true) {
                    val entry = zis.nextFileHeader() ?: break
                    hasReadEntry = true
                    createEntryRAR(zipFileLocation.absolutePath, zis, entry, registry)
                }
                zis.close()
            } catch (e2: IOException) {
                e = e2
                // e.printStackTrace()
            } catch (e2: RarException) {
                e = e2
                // e.printStackTrace()
            }
            return if (hasReadEntry) file else throw e ?: IOException("Zip was empty")
        }

        fun createEntryRAR(
            zipFileLocation: String,
            archive: Archive,
            header: FileHeader,
            registry: HashMap<String, ZipFileRAR>
        ): ZipFileRAR {
            val (parent, path) = ZipCache.splitParent(header.fileNameString)
            val file = registry.getOrPut(path) {
                ZipFileRAR("$zipFileLocation/$path", path, header.isDirectory,
                    registry.getOrPut(parent) { createFolderEntryRAR(zipFileLocation, parent, registry) }
                )
            }
            file.lastModified = header.mTime?.time ?: 0L
            file.lastAccessed = header.aTime?.time ?: 0L
            file.compressedSize = header.fullPackSize
            file.size = header.fullUnpackSize
            if (!file.isDirectory) {
                if (header.isEncrypted) {
                    file.data = "This file is encrypted :/".toByteArray()
                } else {
                    val bos = ByteArrayOutputStream()
                    archive.extractFile(header, bos)
                    bos.close()
                    file.data = bos.toByteArray()
                }
            }
            return file
        }

        fun createFolderEntryRAR(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, ZipFileRAR>
        ): ZipFileRAR {
            val (parent, path) = ZipCache.splitParent(entry)
            val file = registry.getOrPut(path) {
                ZipFileRAR(
                    "$zipFileLocation/$path", path, true,
                    registry.getOrPut(parent) { createFolderEntryRAR(zipFileLocation, parent, registry) }
                )
            }
            file.lastModified = 0L
            file.size = 0
            file.data = null
            return file
        }

    }

}