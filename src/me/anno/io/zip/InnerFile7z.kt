package me.anno.io.zip

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException

class InnerFile7z(
    absolutePath: String,
    val getZipStream: () -> SevenZFile,
    relativePath: String,
    _parent: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent) {

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        val entry = zis.entries.firstOrNull { it.name == relativePath }
        if (entry != null) {
            // target found
            return zis.getInputStream(entry)
        }
        throw FileNotFoundException(relativePath)
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
                    createEntry7z(zipFileLocation.absolutePath, entry, getStream, registry)
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
            zipFileLocation: String,
            entry: SevenZArchiveEntry,
            getStream: () -> SevenZFile,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path) {
                if (entry.isDirectory) {
                    InnerFolder("$zipFileLocation/$path", path, registry[parent]!!)
                } else {
                    InnerFile7z("$zipFileLocation/$path", getStream, path, registry[parent]!!)
                }
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
            file.size = entry.size
            file.data = null
            return file
        }

    }

}