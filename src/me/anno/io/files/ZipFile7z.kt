package me.anno.io.files

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException

class ZipFile7z(
    absolutePath: String,
    val getZipStream: () -> SevenZFile,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

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
        ): ZipFile7z {
            val registry = HashMap<String, ZipFile7z>()
            val file = ZipFile7z(
                zipFileLocation.absolutePath, getStream, "", true,
                zipFileLocation.getParent() ?: zipFileLocation
            )
            registry[file.relativePath] = file
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
            registry: HashMap<String, ZipFile7z>
        ): ZipFile7z {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path){
                ZipFile7z("$zipFileLocation/$path", getStream, path, entry.isDirectory, registry[parent]!!)
            }
            file.lastModified = entry.lastModifiedDate?.time ?: 0L
            file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
            file.size = entry.size
            file.data = null
            return file
        }

    }

}