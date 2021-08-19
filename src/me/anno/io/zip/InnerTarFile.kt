package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.unity.UnityPackage.unpack
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

class InnerTarFile(
    absolutePath: String,
    val getZipStream: () -> ArchiveInputStream,
    relativePath: String,
    _parent: FileReference,
    val readingPath: String = relativePath
) : InnerFile(absolutePath, relativePath, false, _parent) {

    override fun length(): Long = size

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        while (true) {
            val entry = zis.nextEntry ?: break
            if (entry.name == readingPath) {
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
        fun readAsGZip(parent: FileReference): InnerFolder {
            return if (parent.extension.equals("unitypackage", true)) {
                unpack(parent)
            } else {
                // only check if valid, later decode it, when required? may be expensive...
                createZipRegistryArchive(parent) { TarArchiveInputStream(GZIPInputStream(parent.inputStream())) }
            }
        }

        fun createZipRegistryArchive(
            zipFileLocation: FileReference,
            getStream: () -> ArchiveInputStream
        ): InnerFolder {
            val (file, registry) = createMainFolder(zipFileLocation)
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
            return if (hasReadEntry) file else throw e ?: IOException("Zip $zipFileLocation was empty")
        }

        fun createEntryArchive(
            zipFileLocation: String,
            entry: ArchiveEntry,
            zis: ArchiveInputStream,
            getStream: () -> ArchiveInputStream,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                val absolutePath = "$zipFileLocation/$path"
                val parent2 = registry.getOrPut(parent) {
                    createFolderEntryTar(zipFileLocation, parent, registry)
                }
                if (entry.isDirectory) InnerFolder(absolutePath, path, parent2)
                else InnerTarFile(absolutePath, getStream, path, parent2)
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
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry)
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