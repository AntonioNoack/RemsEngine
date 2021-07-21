package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.files.ZipFile7z.Companion.createZipRegistry7z
import me.anno.io.files.ZipFile7z.Companion.fileFromStream7z
import me.anno.io.files.ZipFileRar.Companion.createZipRegistryRar
import me.anno.io.files.ZipFileRar.Companion.fileFromStreamRar
import me.anno.io.files.ZipFileTar.Companion.readAsGZip
import me.anno.io.files.ZipFileV2.Companion.createZipRegistryV2
import me.anno.io.files.ZipFileV2.Companion.fileFromStreamV2
import org.apache.logging.log4j.LogManager

object ZipCache : CacheSection("ZipCache") {

    private val LOGGER = LogManager.getLogger(ZipCache::class)

    // todo cache the whole content? if less than a certain file size
    // todo cache the whole hierarchy? only less than a certain depth level

    // todo read compressed exe files?


    // todo display unity packages differently: display them as their usual file structure
    // it kind of is a new format, that is based on another decompression

    fun getMeta(file: FileReference, async: Boolean): FileReference? {
        val data = getEntry(file.absolutePath, timeout, async) {
            CacheData(try {
                val signature = Signature.find(file)
                when (signature?.name) {
                    "7z" -> createZipRegistry7z(file) { fileFromStream7z(file) }
                    "rar" -> createZipRegistryRar(file) { fileFromStreamRar(file) }
                    "gzip", "tar" -> readAsGZip(file)
                    else -> createZipRegistryV2(file) { fileFromStreamV2(file) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            })
            /* LOGGER.info("Unzipping $file")
             CacheData(try {
                 val extension = file.extension
                 when {
                     extension.equals("7z", true) -> {
                         createZipRegistry7z(file) { fileFromStream7z(file) }
                     }
                     extension.equals("rar", true) -> {
                         createZipRegistryRar(file) { fileFromStreamRar(file) }
                     }
                     else -> {
                         createZipRegistryV2(file) { fileFromStreamV2(file) }
                     }
                 }
             } catch (e: Exception) {
                 // check whether it's a GZip, and if so, decode it
                 try {
                     readAsGZip(file)
                 } catch (e2: Exception) {
                     LOGGER.warn("Error happened")
                     e.printStackTrace()
                     e2.printStackTrace()
                     null
                 }
             })*/
        } as? CacheData<*>
        return data?.value as? FileReference
    }

    fun splitParent(name: String): Pair<String, String> {
        var path = name.replace('\\', '/')
        while (path.endsWith('/')) path = path.substring(0, path.length - 1)
        val nameIndex = path.indexOfLast { it == '/' }
        val parent = if (nameIndex < 0) "" else path.substring(0, nameIndex)
        return parent to path
    }

    val timeout = 10000L
    val sizeLimit = 100_000L

}