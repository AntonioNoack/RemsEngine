package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.HeavyIterator
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFileWithData
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.SignatureFile
import me.anno.io.files.inner.SignatureFile.Companion.setDataAndSignature
import me.anno.io.zip.internal.SevenZHeavyIterator
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.InputStream

class Inner7zFile(
    absolutePath: String,
    val zipFile: FileReference,
    val getZipStream: (Callback<SevenZFile>) -> Unit,
    relativePath: String,
    parent: FileReference
) : InnerFileWithData(absolutePath, relativePath, parent), SignatureFile {

    override var signature: Signature? = null

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        val data = data
        if (data != null) callback.ok(data.inputStream())
        else HeavyIterator.iterate(zipFile, SevenZHeavyIterator(this, callback))
    }

    companion object {

        @JvmStatic
        fun fileFromStream7z(file: FileReference, cb: Callback<SevenZFile>) {
            val file = file.resolved()
            if (file is FileFileRef) {
                cb.ok(SevenZFile(file.file))
            } else {
                file.readBytes(cb.map { bytes ->
                    SevenZFile(SeekableInMemoryByteChannel(bytes))
                })
            }
        }

        @JvmStatic
        fun createZipRegistry7z(
            zipFileLocation: FileReference,
            callback: Callback<InnerFolder>,
            getStream: (Callback<SevenZFile>) -> Unit
        ) {
            getStream { zis, err ->
                if (zis != null) {
                    val (file, registry) = createMainFolder(zipFileLocation)
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        createEntry7z(zis, zipFileLocation, entry, getStream, registry)
                    }
                    callback.ok(file)
                } else callback.err(err)
            }
        }

        @JvmStatic
        fun createEntry7z(
            zis: SevenZFile,
            zipFile: FileReference,
            entry: SevenZArchiveEntry,
            getStream: (Callback<SevenZFile>) -> Unit,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val zipFileLocation = zipFile.absolutePath
                val absolutePath = "$zipFileLocation/$path"
                if (entry.isDirectory) {
                    InnerFolder(absolutePath, path, registry[parent]!!)
                } else {
                    val file = Inner7zFile(absolutePath, zipFile, getStream, path, registry[parent]!!)
                    setDataAndSignature(file, entry.size) { zis.getInputStream(entry) }
                    file
                }
            }
            file.lastModified = if (entry.hasLastModifiedDate) entry.lastModifiedDate!!.time else 0L
            file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
            file.creationTime = if (entry.hasCreationDate) entry.creationDate!!.time else 0L
            return file
        }
    }
}