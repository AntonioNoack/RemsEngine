package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.zip.SignatureFile.Companion.setDataAndSignature
import me.anno.utils.Sleep.waitUntilDefined
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException

class InnerZipFile(
    absolutePath: String,
    val zipSource: FileReference,
    val getZipStream: () -> ZipFile,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    override var signature: Signature? = null

    override fun length(): Long = size

    override fun getInputStream(): InputStream {
        var bytes: ByteArray? = null
        HeavyAccess.access(zipSource, object : IHeavyAccess<ZipFile> {
            override fun openStream(source: FileReference) = getZipStream()
            override fun closeStream(source: FileReference, stream: ZipFile) = stream.close()
            override fun process(stream: ZipFile) {
                val entry = stream.getEntry(relativePath)
                bytes = stream.getInputStream(entry).readBytes()
            }
        })
        return waitUntilDefined(true) { bytes }.inputStream()
    }

    override fun outputStream(): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    companion object {

        fun createFolderEntryV2(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry)
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
            getStream: () -> ZipFile,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val zipFileLocation = zipFile.absolutePath
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, registry) }
                if (entry.isDirectory) InnerFolder(absolutePath, path, parent2)
                else InnerZipFile(absolutePath, zipFile, getStream, path, parent2)
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.size = entry.size
            setDataAndSignature(file) { zis.getInputStream(entry) }
            return file
        }

        fun fileFromStreamV2(file: FileReference): ZipFile {
            return if (file is FileFileRef) {
                ZipFile(file.file)
            } else ZipFile(SeekableInMemoryByteChannel(file.inputStream().readBytes()))
        }

        fun createZipRegistryV2(
            zipFileLocation: FileReference,
            getStream: () -> ZipFile = { fileFromStreamV2(zipFileLocation) }
        ): InnerFolder {
            val (file, registry) = createMainFolder(zipFileLocation)
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                val entries = zis.entries
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    hasReadEntry = true
                    createEntryV2(zipFileLocation, entry, zis, getStream, registry)
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

    }

}