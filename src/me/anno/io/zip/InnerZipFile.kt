package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException

class InnerZipFile(
    absolutePath: String,
    val getZipStream: () -> ZipFile,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent) {

    override fun length(): Long = size

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        return zis.getInputStream(zis.getEntry(relativePath))
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
            zipFileLocation: String,
            entry: ZipArchiveEntry,
            zis: ZipFile,
            getStream: () -> ZipFile,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, registry) }
                if (entry.isDirectory) InnerFolder(absolutePath, path, parent2)
                else InnerZipFile(absolutePath, getStream, path, parent2)
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.size = entry.size
            file.data = if (!file.isDirectory && file.size in 1..ZipCache.sizeLimit) {
                zis.getInputStream(entry).readBytes()
            } else null
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
                    createEntryV2(zipFileLocation.absolutePath, entry, zis, getStream, registry)
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