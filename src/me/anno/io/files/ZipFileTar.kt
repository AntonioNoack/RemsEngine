package me.anno.io.files

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

class ZipFileTar(
    absolutePath: String,
    val getZipStream: () -> ArchiveInputStream,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun length(): Long = size

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        while (true) {
            val entry = zis.nextEntry ?: break
            if (entry.name == relativePath) {
                return zis
            }
        }
        throw IOException("Not found")
    }

    override fun outputStream(): OutputStream {
        throw RuntimeException("Writing into zip files is not yet supported")
    }

    companion object {

        // assumes tar.gz format
        fun readAsGZip(parent: FileReference): ZipFileBase {
            // only check if valid, later decode it, when required? may be expensive...
            return createZipRegistryArchive(parent) { TarArchiveInputStream(GZIPInputStream(parent.inputStream())) }
        }

        fun createZipRegistryArchive(
            zipFileLocation: FileReference,
            getStream: () -> ArchiveInputStream
        ): ZipFileTar {
            val registry = HashMap<String, ZipFileTar>()
            val file = ZipFileTar(
                zipFileLocation.absolutePath, getStream, "", true,
                zipFileLocation.getParent() ?: zipFileLocation
            )
            registry[""] = file
            var hasReadEntry = false
            var e: Exception? = null
            try {
                val zis = getStream()
                while (true) {
                    val entry = zis.nextEntry ?: break
                    hasReadEntry = true
                    createEntryArchive(zipFileLocation.absolutePath, entry, zis, getStream, registry)
                }
                zis.close()
            } catch (e2: IOException) {
                e = e2
            } catch (e2: ZipException) {
                e = e2
            }
            return if (hasReadEntry) file else throw e ?: IOException("Zip was empty")
        }

        fun createEntryArchive(
            zipFileLocation: String,
            entry: ArchiveEntry,
            zis: ArchiveInputStream,
            getStream: () -> ArchiveInputStream,
            registry: HashMap<String, ZipFileTar>
        ): ZipFileTar {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                ZipFileTar(
                    "$zipFileLocation/$path", getStream, path, entry.isDirectory,
                    registry.getOrPut(parent) {
                        createFolderEntryTar(zipFileLocation, parent, getStream, registry)
                    }
                )
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.size = entry.size
            file.data = if (!file.isDirectory && file.size in 1..ZipCache.sizeLimit) {
                zis.readBytes()
            } else null
            return file
        }

        fun createFolderEntryTar(
            zipFileLocation: String,
            entry: String,
            getStream: () -> ArchiveInputStream,
            registry: HashMap<String, ZipFileTar>
        ): ZipFileTar {
            val (parent, path) = ZipCache.splitParent(entry)
            val file = registry.getOrPut(path) {
                ZipFileTar(
                    "$zipFileLocation/$path", getStream, path, true,
                    registry.getOrPut(parent) { createFolderEntryTar(zipFileLocation, parent, getStream, registry) }
                )
            }
            file.lastModified = 0L
            val parent2 = file._parent as ZipFileBase
            parent2.children!![file.lcName] = file
            file.size = 0
            file.data = null
            return file
        }

    }

}