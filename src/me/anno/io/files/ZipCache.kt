package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

object ZipCache : CacheSection("ZipCache") {

    // todo cache the whole content? if less than a certain file size
    // todo cache the whole hierarchy? only less than a certain depth level

    // todo read compressed exe files?

    fun getMeta2(file: FileReference, async: Boolean): FileReference? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(if (file.extension.equals("7z", true)) {
                createZipRegistry7z(file) { fileFromStream7z(file) }
            } else {
                createZipRegistryV2(file) { fileFromStreamV2(file) }
            })
        } as? CacheData<*>
        return data?.value as? FileReference
    }

    fun fileFromStreamV2(file: FileReference): ZipFile {
        return if (file is FileFileRef) {
            ZipFile(file.file)
        } else ZipFile(SeekableInMemoryByteChannel(file.inputStream().readBytes()))
    }

    fun fileFromStream7z(file: FileReference): SevenZFile {
        return if (file is FileFileRef) {
            SevenZFile(file.file)
        } else {
            SevenZFile(SeekableInMemoryByteChannel(file.inputStream().readBytes()))
        }
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
        registry[file.relativePath] = file
        try {
            val zis = getStream()
            val entries = zis.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                createEntryV2(zipFileLocation.absolutePath, entry, zis, getStream, registry)
            }
            zis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ZipException) {
            e.printStackTrace()
        }
        return file
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
        try {
            val zis = getStream()
            while (true) {
                val entry = zis.nextEntry ?: break
                createEntry7z(zipFileLocation.absolutePath, entry, getStream, registry)
            }
            zis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ZipException) {
            e.printStackTrace()
        }
        return file
    }

    fun createEntry7z(
        zipFileLocation: String,
        entry: SevenZArchiveEntry,
        getStream: () -> SevenZFile,
        registry: HashMap<String, ZipFile7z>
    ): ZipFile7z {
        val (parent, path) = splitParent(entry.name)
        val file = ZipFile7z("$zipFileLocation/$path", getStream, path, entry.isDirectory, registry[parent]!!)
        file.lastModified = entry.lastModifiedDate?.time ?: 0L
        file.lastAccessed = if (entry.hasAccessDate) entry.accessDate!!.time else 0L
        val parent2 = file._parent as? ZipFile7z
        if (parent2 != null && parent2.isDirectory) {
            parent2.children!![file.lcName] = file
        }
        file.size = entry.size
        file.data = null
        registry[file.relativePath] = file
        return file
    }

    fun createEntryV2(
        zipFileLocation: String,
        entry: ZipArchiveEntry,
        zis: ZipFile,
        getStream: () -> ZipFile,
        registry: HashMap<String, ZipFileV2>
    ): ZipFileV2 {
        val (parent, path) = splitParent(entry.name)
        val file = ZipFileV2("$zipFileLocation/$path", getStream, path, entry.isDirectory, registry[parent]!!)
        file.lastModified = entry.lastModifiedDate?.time ?: 0L
        val parent2 = file._parent as? ZipFileV2
        if (parent2 != null && parent2.isDirectory) {
            parent2.children!![file.lcName] = file
        }
        file.size = entry.size
        file.data = if (!file.isDirectory && file.size in 1..sizeLimit) {
            val bytes = zis.getInputStream(entry).readBytes()
            bytes
        } else null
        registry[file.relativePath] = file
        return file
    }

    fun getMeta(file: File, async: Boolean): ZipFileV1? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(createZipRegistry(FileReference.getReference(file)) { ZipInputStream(file.inputStream()) })
        } as? CacheData<*>
        return data?.value as? ZipFileV1
    }

    fun getMeta(file: FileReference, async: Boolean): ZipFileV1? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(createZipRegistry(file) { ZipInputStream(file.inputStream()) })
        } as? CacheData<*>
        return data?.value as? ZipFileV1
    }

    fun createZipRegistry(zipFileLocation: FileReference, getZis: () -> ZipInputStream): ZipFileV1 {
        val registry = HashMap<String, ZipFileV1>()
        val file = ZipFileV1(zipFileLocation.absolutePath, getZis, "", true, zipFileLocation)
        registry[file.relativePath] = file
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

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        if (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        return parent to path
    }

    fun createEntry(
        zipFileLocation: String,
        entry: ZipEntry,
        zis: ZipInputStream,
        getZis: () -> ZipInputStream,
        registry: HashMap<String, ZipFileV1>
    ): ZipFileV1 {
        val (parent, path) = splitParent(entry.name)
        val file = ZipFileV1("$zipFileLocation/$path", getZis, path, entry.isDirectory, registry[parent]!!)
        file.lastAccessed = entry.lastAccessTime?.toMillis() ?: 0L
        file.lastModified = entry.lastModifiedTime?.toMillis() ?: 0L
        val parent2 = file._parent as? ZipFileBase
        if (parent2 != null && parent2.isDirectory) {
            parent2.children!![file.lcName] = file
        }
        file.size = entry.size
        file.compressedSize = entry.compressedSize
        file.data = if (!file.isDirectory && file.size in 1..sizeLimit) {
            val bytes = zis.readBytes()
            bytes
        } else null
        registry[file.relativePath] = file
        return file
    }

    val timeout = 10000L
    val sizeLimit = 100_000L

}