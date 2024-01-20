package me.anno.io.zip

import me.anno.utils.structures.Callback
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.*
import me.anno.io.files.inner.SignatureFile.Companion.setDataAndSignature
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class InnerZipFile(
    absolutePath: String,
    val zipSource: FileReference,
    val getZipStream: (callback: Callback<ZipFile>) -> Unit,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    override var signature: Signature? = null

    override fun length(): Long = size

    override fun inputStream(lengthLimit: Long, callback: Callback<InputStream>) {
        HeavyAccess.access(zipSource, object : IHeavyAccess<ZipFile> {

            override fun openStream(source: FileReference, callback: Callback<ZipFile>) =
                getZipStream(callback)

            override fun closeStream(source: FileReference, stream: ZipFile) = stream.close()

            override fun process(stream: ZipFile) {
                val entry = stream.getEntry(relativePath)
                callback.ok(stream.getInputStream(entry).readBytes().inputStream())
            }
        }) { callback.err(it) }
    }

    override fun outputStream(append: Boolean): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    companion object {

        fun createFolderEntryV2(
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

        fun createEntryV2(
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
                else InnerZipFile(absolutePath, zipFile, getStream, path, parent2)
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.creationTime = entry.creationTime?.toMillis() ?: 0L
            file.size = entry.size
            setDataAndSignature(file) { zis.getInputStream(entry) }
            return file
        }

        fun fileFromStreamV2(file: FileReference, callback: Callback<ZipFile>) {
            return if (file is FileFileRef) {
                callback.ok(ZipFile(file.file))
            } else {
                file.readBytes { it, exc ->
                    if (it != null) callback.ok(ZipFile(SeekableInMemoryByteChannel(it)))
                    else callback.err(exc)
                }
            }
        }

        fun createZipRegistryV2(
            file0: FileReference,
            callback: Callback<InnerFolder>,
            getStream: (Callback<ZipFile>) -> Unit = { fileFromStreamV2(file0, it) }
        ) {
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
            return
        }
    }
}