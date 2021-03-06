package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.zip.SignatureFile.Companion.setDataAndSignature
import me.anno.utils.Sleep.waitUntilDefined
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException

class InnerFile7z(
    absolutePath: String,
    val zipFile: FileReference,
    val getZipStream: () -> SevenZFile,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    override var signature: Signature? = null

    class Iterate7z(val file: SevenZFile) : NextEntryIterator<SevenZArchiveEntry>() {
        override fun nextEntry(): SevenZArchiveEntry? = file.nextEntry
    }

    override fun getInputStream(): InputStream {
        var bytes: ByteArray? = null
        HeavyIterator.iterate(zipFile, object : IHeavyIterable<SevenZArchiveEntry, Iterate7z, ByteArray> {
            override fun openStream(source: FileReference) = Iterate7z(getZipStream())
            override fun hasInterest(stream: Iterate7z, item: SevenZArchiveEntry) =
                item.name == relativePath

            override fun process(
                stream: Iterate7z,
                item: SevenZArchiveEntry,
                previous: ByteArray?,
                index: Int,
                total: Int
            ): ByteArray? {
                bytes = previous ?: stream.file.getInputStream(item).readBytes()
                return bytes
            }

            override fun closeStream(source: FileReference, stream: Iterate7z) {
                stream.file.close()
            }

        })
        return waitUntilDefined(true) { bytes }.inputStream()
    }

    companion object {

        fun fileFromStream7z(file: FileReference): SevenZFile {
            return if (file is FileFileRef) {
                SevenZFile(file.file)
            } else {
                SevenZFile(SeekableInMemoryByteChannel(file.inputStream().readBytes()))
            }
        }

        fun createZipRegistry7z(
            zipFileLocation: FileReference,
            getStream: () -> SevenZFile
        ): InnerFolder {
            val (file, registry) = createMainFolder(zipFileLocation)
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                while (true) {
                    val entry = zis.nextEntry ?: break
                    hasReadEntry = true
                    createEntry7z(zis, zipFileLocation, entry, getStream, registry)
                }
                zis.close()
            } catch (e2: IOException) {
                e = e2
                // e.printStackTrace()
            } catch (e2: ZipException) {
                e = e2
                // e.printStackTrace()
            }
            return if (hasReadEntry) file else throw e ?: IOException("Zip was empty")
        }

        fun createEntry7z(
            zis: SevenZFile,
            zipFile: FileReference,
            entry: SevenZArchiveEntry,
            getStream: () -> SevenZFile,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val zipFileLocation = zipFile.absolutePath
                if (entry.isDirectory) {
                    InnerFolder("$zipFileLocation/$path", path, registry[parent]!!)
                } else {
                    InnerFile7z("$zipFileLocation/$path", zipFile, getStream, path, registry[parent]!!)
                }
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
            file.size = entry.size
            setDataAndSignature(file) { zis.getInputStream(entry) }
            return file
        }

    }

}