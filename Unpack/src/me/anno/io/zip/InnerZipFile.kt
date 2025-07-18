package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.HeavyAccess
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFileWithData
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.SignatureFile
import me.anno.io.files.inner.SignatureFile.Companion.setDataAndSignature
import me.anno.io.zip.internal.ZipHeavyAccess
import me.anno.utils.async.Callback
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream

class InnerZipFile(
    absolutePath: String,
    val zipSource: FileReference,
    val getZipStream: (callback: Callback<ZipFile>) -> Unit,
    relativePath: String,
    parent: FileReference
) : InnerFileWithData(absolutePath, relativePath, parent), SignatureFile {

    override var signature: Signature? = null

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        val data = data
        if (data != null) callback.ok(data.inputStream())
        else HeavyAccess.access(zipSource, ZipHeavyAccess(this, callback)) { callback.err(it) }
    }

    companion object {

        private fun createFolderEntryV2(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry)
            return registry.getOrPut(path) {
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, registry) }
                InnerFolder(absolutePath, path, parent2)
            }
        }

        private fun createEntryV2(
            zipFile: FileReference,
            entry: ZipArchiveEntry,
            zis: ZipFile,
            getStream: (Callback<ZipFile>) -> Unit,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val zipFileLocation = zipFile.absolutePath
            val (parent, path) = InnerFolderCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, registry) }
                if (entry.isDirectory) InnerFolder(absolutePath, path, parent2)
                else {
                    val file = InnerZipFile(absolutePath, zipFile, getStream, path, parent2)
                    setDataAndSignature(file, entry.size) { zis.getInputStream(entry) }
                    file
                }
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.creationTime = entry.creationTime?.toMillis() ?: 0L
            return file
        }

        private fun zipFileFromFile(file: FileReference, callback: Callback<ZipFile>) {
            val file = file.resolved()
            if (file is FileFileRef) {
                callback.ok(ZipFile(file.file))
            } else {
                file.readBytes { bytes, exc ->
                    if (bytes != null) zipFileFromBytes(bytes, callback)
                    exc?.printStackTrace()
                }
            }
        }

        private fun zipFileFromBytes(bytes: ByteArray, callback: Callback<ZipFile>) {
            callback.ok(ZipFile(SeekableInMemoryByteChannel(bytes)))
        }

        fun createZipRegistryV2(
            file0: FileReference, bytes: ByteArray,
            callback: Callback<InnerFolder>
        ) = createZipRegistryV3(file0, callback) { zipFileFromBytes(bytes, it) }

        fun createZipRegistryV2(
            file0: FileReference,
            callback: Callback<InnerFolder>
        ) = createZipRegistryV3(file0, callback) { zipFileFromFile(file0, it) }

        private fun createZipRegistryV3(
            file0: FileReference,
            callback: Callback<InnerFolder>,
            getStream: (Callback<ZipFile>) -> Unit
        ) {
            if (file0 is FileFileRef) {
                return InnerZipFileV2.createZipFile(file0, callback)
            }

            val (file, registry) = createMainFolder(file0)
            var hasReadEntry = false
            getStream { zis, exc ->
                if (zis != null) {
                    val entries = zis.entries
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        hasReadEntry = true
                        createEntryV2(file, entry, zis, getStream, registry)
                    }
                    zis.close()
                    if (hasReadEntry) {
                        // create fast lookup to find missing files,
                        // e.g., inside synty store fbx files
                        val lookup = HashMap<String, InnerFile>(registry.size)
                        file.lookup = lookup
                        for ((_, value) in registry) {
                            lookup[value.name] = value
                            lookup.getOrPut(value.nameWithoutExtension) { value }
                            if (value is InnerFolder) value.lookup = lookup
                        }
                        callback.ok(file)
                    } else {
                        callback.err(IOException("Zip was empty"))
                    }
                } else callback.err(exc)
            }
        }
    }
}