package me.anno.io.zip

import com.github.junrar.Archive
import com.github.junrar.Volume
import com.github.junrar.exception.RarException
import com.github.junrar.io.IReadOnlyAccess
import com.github.junrar.rarfile.FileHeader
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class InnerRarFile(
    absolutePath: String,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    override var signature: Signature? = null

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(data!!.inputStream(),null)
    }

    override fun inputStreamSync(): InputStream {
        return data!!.inputStream()
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
        val bytes = lazy { file.readBytesSync() }
        override fun getReadOnlyAccess(): IReadOnlyAccess {
            return ZipReadOnlyAccess(bytes.value)
        }

        override fun getLength(): Long = file.length()
        override fun getArchive(): Archive = a
    }

    companion object {

        fun fileFromStreamRar(file: FileReference): Archive {
            return if (file is FileFileRef) {
                Archive(file.file)
            } else {
                // probably for loading a set of files,
                // which will/would be combined into a single one
                Archive { a, v ->
                    // see FileArchive as an example
                    if (v == null) {
                        ZipVolume(a, file)
                    } else TODO("joined rar-s not yet supported")
                }
            }
        }

        fun createZipRegistryRar(
            zipFileLocation: FileReference,
            getStream: () -> Archive
        ): InnerFolder {
            val (file, registry) = createMainFolder(zipFileLocation)
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                if (zis.isEncrypted) {
                    file.isEncrypted = true
                    throw IOException("RAR is encrypted")
                }
                while (true) {
                    val entry = zis.nextFileHeader() ?: break
                    hasReadEntry = true
                    createEntryRar(zipFileLocation.absolutePath, zis, entry, registry)
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

        private fun createEntryRar(
            zipFileLocation: String,
            archive: Archive,
            header: FileHeader,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(header.fileNameString)
            val file = registry.getOrPut(path) {
                val parent2 = registry.getOrPut(parent) { createFolderEntryRar(zipFileLocation, parent, registry) }
                if (header.isDirectory) {
                    InnerFolder("$zipFileLocation/$path", path, parent2)
                } else {
                    InnerRarFile("$zipFileLocation/$path", path, parent2)
                }
            }
            file.lastModified = header.mTime?.time ?: 0L
            file.lastAccessed = header.aTime?.time ?: 0L
            file.compressedSize = header.fullPackSize
            file.size = header.fullUnpackSize
            file.isEncrypted = header.isEncrypted
            if (!file.isDirectory) {
                if (file.isEncrypted) {
                    file.data = "This file is encrypted :/".toByteArray()
                } else {
                    val bos = ByteArrayOutputStream()
                    archive.extractFile(header, bos)
                    bos.close()
                    val data = bos.toByteArray()
                    file.data = data
                    file as SignatureFile
                    file.signature = Signature.find(data)
                }
            }
            return file
        }

        private fun createFolderEntryRar(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry)
            val file = registry.getOrPut(path) {
                val parent2 = registry.getOrPut(parent) { createFolderEntryRar(zipFileLocation, parent, registry) }
                InnerFolder("$zipFileLocation/$path", path, parent2)
            }
            file.lastModified = 0L
            file.size = 0
            file.data = null
            return file
        }

    }

}