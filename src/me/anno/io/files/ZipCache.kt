package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

object ZipCache : CacheSection("ZipCache") {

    // todo cache the whole content? if less than a certain file size
    // todo cache the whole hierarchy? only less than a certain depth level

    fun getMeta(file: File, async: Boolean): ZipFile? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(createZipRegistry(file.absolutePath) { ZipInputStream(file.inputStream()) })
        } as? CacheData<*>
        return data?.value as? ZipFile
    }

    fun getMeta(file: FileReference, async: Boolean): ZipFile? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(createZipRegistry(file.absolutePath) { ZipInputStream(file.inputStream()) })
        } as? CacheData<*>
        return data?.value as? ZipFile
    }

    fun createZipRegistry(zipFileLocation: String, getZis: () -> ZipInputStream): ZipFile {
        // todo read compressed exe files?
        val registry = HashMap<String, ZipFile>()
        val file = ZipFile(zipFileLocation, getZis, "", true, null)
        registry[file.relativePath] = file
        try {
            val zis = getZis()
            while (true) {
                val entry = zis.nextEntry ?: break
                createEntry(zipFileLocation, entry, zis, getZis, registry)
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
        registry: HashMap<String, ZipFile>
    ): ZipFile {
        var path = entry.name.replace('\\', '/')
        if (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val name = if (nameIndex < 0) path else path.substring(nameIndex + 1)
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        val isDirectory = entry.isDirectory
        val file = ZipFile("$zipFileLocation/$path", getZis, path, isDirectory, registry[parent])
        file.lastAccessed = entry.lastAccessTime?.toMillis() ?: 0L
        file.lastModified = entry.lastModifiedTime?.toMillis() ?: 0L
        val parent2 = file.parent
        if (parent2 != null && parent2.isDirectory) {
            parent2.children!![file.lcName] = file
            // parent2.children[file.name] = file
        }
        file.size = entry.size
        file.compressedSize = entry.compressedSize
        file.data = if (file.size in 1..sizeLimit) {
            val bytes = zis.readBytes()
            bytes
        } else null
        registry[file.relativePath] = file
        return file
    }

    val timeout = 10000L
    val sizeLimit = 100_000L

}