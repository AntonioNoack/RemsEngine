package me.anno.cache

import me.anno.Engine.startDateTime
import me.anno.cache.instances.LastModifiedCache
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

abstract class FileCache<Key>(val configFileName: String, val configFolderName: String, cacheName: String) :
    CacheSection(cacheName) {

    // create a metadata file, where last used (Rem's Studio starting time) is written
    lateinit var info: StringMap

    // check all last instances, which can be deleted...
    lateinit var proxyFolder: FileReference

    var isInitialized = false
    fun init() {
        if (isInitialized) return
        isInitialized = true
        info = ConfigBasics.loadConfig(configFileName, InvalidRef, StringMap(), false)
        proxyFolder = ConfigBasics.cacheFolder.getChild(configFolderName).apply { tryMkdirs() }
        proxyFolder.mkdirs()
        deleteUnusedFiles()
    }

    // src1.exists && !src1.isDirectory
    open fun isKeyValid(key: Key): Boolean = true

    fun generateFile(key: Key): CacheData<FileReference?> {
        return if (isKeyValid(key)) {
            val uuid = getUniqueFilename(key)
            val dst = proxyFolder.getChild(uuid)
            val data = CacheData<FileReference?>(null)
            if (!dst.exists) {
                val tmp = proxyFolder.getChild(dst.nameWithoutExtension + ".tmp.${dst.extension}")
                LOGGER.debug("$key -> $tmp -> $dst")
                fillFileContents(key, tmp, {
                    postCreateProxy(uuid, tmp, dst)
                    data.value = dst
                }, {
                    it?.printStackTrace()
                    data.value = InvalidRef
                })
            } else {
                markUsed(uuid)
                data.value = dst
            }
            data
        } else CacheData(null)
    }

    fun markUsed(uuid: String) {
        init()
        // LOGGER.info("Mark used: $uuid = $startDateTime")
        info[uuid] = startDateTime
        info.saveMaybe(configFileName)
    }

    fun postCreateProxy(uuid: String, tmp: FileReference, dst: FileReference): Boolean {
        LastModifiedCache.invalidate(tmp)
        if (tmp.exists) {
            if (dst.exists) dst.deleteRecursively()
            tmp.renameTo(dst)
            if (dst.exists) {
                markUsed(uuid)
                return true
            } else LOGGER.warn("$dst is somehow missing")
        } else LOGGER.warn("$tmp is somehow missing")
        return false
    }

    /**
     * scales video down 4x
     * */
    abstract fun fillFileContents(
        key: Key, dst: FileReference,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit,
    )

    abstract fun getUniqueFilename(key: Key): String

    private fun deleteUnusedFiles() {
        var deleted = 0
        var kept = 0
        var freed = 0L
        val children = proxyFolder.listChildren()
        if (children != null) for (file in children) {
            if (!file.isDirectory && abs(info[file.name, file.lastModified] - startDateTime) > proxyValidityTimeout) {
                freed += file.length()
                file.delete()
                deleted++
            } else {
                kept++
            }
        }
        if (deleted > 0) LOGGER.info("[$name] Deleted ${deleted}/${deleted + kept} files, which haven't been used in the last 7 days. Freed ${freed.formatFileSize()}")
        info.removeIf { (_, value) -> value !is Long || abs(value - startDateTime) > proxyValidityTimeout }
    }

    companion object {
        private const val proxyValidityTimeout = 7 * 24 * 3600 * 1000 // delete files after one week of not-used
        private val LOGGER = LogManager.getLogger(FileCache::class)
    }


}