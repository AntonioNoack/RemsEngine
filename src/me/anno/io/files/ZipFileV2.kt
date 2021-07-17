package me.anno.io.files

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException

class ZipFileV2(
    absolutePath: String,
    val getZipStream: () -> ZipFile,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

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
            getStream: () -> ZipFile,
            registry: HashMap<String, ZipFileV2>
        ): ZipFileV2 {
            val (parent, path) = ZipCache.splitParent(entry)
            val file = registry.getOrPut(path){
                ZipFileV2(
                    "$zipFileLocation/$path", getStream, path, true,
                    registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, getStream, registry) }
                )
            }
            file.lastModified = 0L
            file.size = 0
            file.data = null
            return file
        }

        fun createEntryV2(
            zipFileLocation: String,
            entry: ZipArchiveEntry,
            zis: ZipFile,
            getStream: () -> ZipFile,
            registry: HashMap<String, ZipFileV2>
        ): ZipFileV2 {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path){
                ZipFileV2(
                    "$zipFileLocation/$path", getStream, path, entry.isDirectory,
                    registry.getOrPut(parent) { createFolderEntryV2(zipFileLocation, parent, getStream, registry) }
                )
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
            getStream: () -> ZipFile
        ): ZipFileV2 {
            val registry = HashMap<String, ZipFileV2>()
            val file = ZipFileV2(
                zipFileLocation.absolutePath, getStream, "", true,
                zipFileLocation.getParent() ?: zipFileLocation
            )
            registry[""] = file
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