package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.Signature
import me.anno.io.files.inner.*
import me.anno.io.files.inner.SignatureFile.Companion.setDataAndSignature
import me.anno.io.unity.UnityPackage.unpack
import me.anno.utils.structures.NextEntryIterator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

class InnerTarFile(
    absolutePath: String,
    val zipFile: FileReference,
    val getZipStream: () -> ArchiveInputStream,
    relativePath: String,
    _parent: FileReference,
    val readingPath: String = relativePath
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    override var signature: Signature? = null

    override fun length(): Long = size

    class ZipArchiveIterator(val file: ArchiveInputStream) : NextEntryIterator<ArchiveEntry>() {
        override fun nextEntry(): ArchiveEntry? = file.nextEntry
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        HeavyIterator.iterate(zipFile, object : IHeavyIterable<ArchiveEntry, ZipArchiveIterator, ByteArray> {
            override fun openStream(source: FileReference) = ZipArchiveIterator(getZipStream())
            override fun closeStream(source: FileReference, stream: ZipArchiveIterator) = stream.file.close()
            override fun hasInterest(stream: ZipArchiveIterator, item: ArchiveEntry) = item.name == readingPath

            override fun process(
                stream: ZipArchiveIterator,
                item: ArchiveEntry, previous: ByteArray?,
                index: Int, total: Int
            ): ByteArray {
                val bytes = previous ?: stream.file.readBytes()
                callback(bytes.inputStream(), null)
                return bytes
            }
        })
    }

    override fun outputStream(append: Boolean): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    companion object {

        // assumes tar.gz format
        fun readAsGZip(parent: FileReference, callback: InnerFolderCallback) {
            if (parent.lcExtension == "unitypackage") {
                unpack(parent, callback)
            } else {
                // only check if valid, later decode it, when required? may be expensive...
                parent.inputStream { it, exc ->
                    if (it != null) {
                        callback(createZipRegistryArchive(parent) {
                            TarArchiveInputStream(GZIPInputStream(it))
                        }, null)
                    } else callback(null, exc)
                }
            }
        }

        fun createZipRegistryArchive(
            zipFileLocation: FileReference,
            getStream: () -> ArchiveInputStream
        ): InnerFolder {
            val (file, registry) = createMainFolder(zipFileLocation)
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                while (true) {
                    val entry = zis.nextEntry ?: break
                    hasReadEntry = true
                    createEntryArchive(zipFileLocation, entry, zis, getStream, registry)
                }
                zis.close()
            } catch (e2: IOException) {
                e = e2
            } catch (e2: ZipException) {
                e = e2
            }
            return if (hasReadEntry) file else throw e ?: IOException("Zip $zipFileLocation was empty")
        }

        fun createEntryArchive(
            zipFile: FileReference,
            entry: ArchiveEntry,
            zis: ArchiveInputStream,
            getStream: () -> ArchiveInputStream,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val zipFileLocation = zipFile.absolutePath
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) {
                    createFolderEntryTar(zipFileLocation, parent, registry)
                }
                if (entry.isDirectory) InnerFolder(absolutePath, path, parent2)
                else InnerTarFile(absolutePath, zipFile, getStream, path, parent2)
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.size = entry.size
            setDataAndSignature(file) {
                // stream must not be closed
                object : InputStream() {
                    override fun read(): Int = zis.read()
                    override fun read(p0: ByteArray): Int {
                        return zis.read(p0)
                    }

                    override fun read(p0: ByteArray, p1: Int, p2: Int): Int {
                        return zis.read(p0, p1, p2)
                    }
                }
            }
            return file
        }

        fun createFolderEntryTar(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry)
            /*file.lastModified = 0L
            val parent2 = file._parent as InnerFolder
            parent2.children[file.lcName] = file
            file.size = 0
            file.data = null*/
            return registry.getOrPut(path) {
                val parent2 = registry.getOrPut(parent) { createFolderEntryTar(zipFileLocation, parent, registry) }
                InnerFolder("$zipFileLocation/$path", path, parent2)
            }
        }
    }
}