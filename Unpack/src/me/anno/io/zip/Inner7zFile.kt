package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.HeavyIterator
import me.anno.io.files.inner.InnerFile
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
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

    override var signature: Signature? = null

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        if (data != null) super.inputStream(lengthLimit, closeStream, callback)
        else HeavyIterator.iterate(zipFile, SevenZHeavyIterator(this, callback))
    }

    companion object {

        @JvmStatic
        fun fileFromStream7z(file: FileReference, cb: Callback<SevenZFile>) {
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
                if (entry.isDirectory) {
                    InnerFolder("$zipFileLocation/$path", path, registry[parent]!!)
                } else {
                    Inner7zFile("$zipFileLocation/$path", zipFile, getStream, path, registry[parent]!!)
                }
            }
            file.lastModified = if (entry.hasLastModifiedDate) entry.lastModifiedDate!!.time else 0L
            file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
            file.creationTime = if (entry.hasCreationDate) entry.creationDate!!.time else 0L
            file.size = entry.size
            setDataAndSignature(file) { zis.getInputStream(entry) }
            return file
        }
    }
}