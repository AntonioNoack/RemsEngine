package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.HeavyIterator
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.io.files.inner.SignatureFile
import me.anno.io.files.inner.SignatureFile.Companion.setDataAndSignature
import me.anno.io.unity.UnityPackage.unpack
import me.anno.io.zip.internal.TarHeavyIterator
import me.anno.utils.async.Callback
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

class InnerTarFile(
    absolutePath: String,
    val zipFile: FileReference,
    val getZipStream: (callback: Callback<ArchiveInputStream>) -> Unit,
    relativePath: String,
    parent: FileReference,
    val readingPath: String = relativePath
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

    override var signature: Signature? = null

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        if (data != null) super.inputStream(lengthLimit, closeStream, callback)
        else HeavyIterator.iterate(zipFile, TarHeavyIterator(this, callback))
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
                        createTarRegistryArchive(parent, callback) { callback ->
                            callback.ok(TarArchiveInputStream(GZIPInputStream(it)))
                        }
                    } else callback.err(exc)
                }
            }
        }

        fun createTarRegistryArchive(
            zipFileLocation: FileReference,
            callback: Callback<InnerFolder>,
            getStream: (Callback<ArchiveInputStream>) -> Unit,
        ) {
            getStream { zis, err ->
                if (zis != null) {
                    val (file, registry) = createMainFolder(zipFileLocation)
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        createEntryArchive(zipFileLocation, entry, zis, getStream, registry)
                    }
                    callback.ok(file)
                } else callback.err(err)
            }
        }

        fun createEntryArchive(
            zipFile: FileReference,
            entry: ArchiveEntry,
            zis: ArchiveInputStream,
            getStream: (Callback<ArchiveInputStream>) -> Unit,
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