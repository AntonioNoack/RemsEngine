package me.anno.io.files

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

class ZipFileV1(
    absolutePath: String,
    val getZipStream: () -> ZipInputStream,
    relativePath: String,
    isDirectory: Boolean,
    _parent: FileReference
) : ZipFileBase(absolutePath, relativePath, isDirectory, _parent) {

    override fun getInputStream(): InputStream {
        val zis = getZipStream()
        while (true) {
            val entry = zis.nextEntry ?: break
            if (entry.name == relativePath) {
                // target found
                return zis
            }
        }
        throw FileNotFoundException(relativePath)
    }

    companion object {

        fun createZipRegistry(zipFileLocation: FileReference, getZis: () -> ZipInputStream): ZipFileV1 {
            val registry = HashMap<String, ZipFileV1>()
            val file = ZipFileV1(zipFileLocation.absolutePath, getZis, "", true, zipFileLocation)
            registry[""] = file
            try {
                val zis = getZis()
                while (true) {
                    val entry = zis.nextEntry ?: break
                    createEntry(zipFileLocation.absolutePath, entry, zis, getZis, registry)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ZipException) {
                e.printStackTrace()
            }
            return file
        }

        fun createEntry(
            zipFileLocation: String,
            entry: ZipEntry,
            zis: ZipInputStream,
            getZis: () -> ZipInputStream,
            registry: HashMap<String, ZipFileV1>
        ): ZipFileV1 {
            val (parent, path) = ZipCache.splitParent(entry.name)
            val file = registry.getOrPut(path){
                ZipFileV1("$zipFileLocation/$path", getZis, path, entry.isDirectory, registry[parent]!!)
            }
            file.lastAccessed = entry.lastAccessTime?.toMillis() ?: 0L
            file.lastModified = entry.lastModifiedTime?.toMillis() ?: 0L
            file.size = entry.size
            file.compressedSize = entry.compressedSize
            file.data = if (!file.isDirectory && file.size in 1..ZipCache.sizeLimit) {
                zis.readBytes()
            } else null
            return file
        }

    }

}