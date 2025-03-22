package me.anno.cache

import me.anno.Time.startDateTime
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.LastModifiedCache
import me.anno.io.utils.StringMap
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

/**
 * a cache, which writes files to remember values
 * */
abstract class FileCache<Key, Value>(
    val configFileName: String,
    val configFolderName: String,
    cacheName: String
) : CacheSection(cacheName) {

    // create a metadata file, where last used (Rem's Studio starting time) is written
    lateinit var info: StringMap

    // check all last instances, which can be deleted...
    lateinit var cacheFolder: FileReference

    var isInitialized = false
    fun init() {
        if (isInitialized) return
        isInitialized = true
        info = ConfigBasics.loadConfig(configFileName, InvalidRef, StringMap(), false)
        cacheFolder = ConfigBasics.cacheFolder.getChild(configFolderName).apply { tryMkdirs() }
        cacheFolder.mkdirs()
        deleteUnusedFiles()
    }

    // src1.exists && !src1.isDirectory
    abstract fun load(key: Key, src: FileReference?): Value

    fun getFile(uniqueFileName: String) = cacheFolder.getChild(uniqueFileName)

    fun generateFile(key: Key): AsyncCacheData<Value?> {
        init()

        val data = AsyncCacheData<Value?>()
        val uuid = getUniqueFilename(key)
        if (uuid == null) {
            data.value = null
            return data
        }

        val dst = getFile(uuid)
        if (!dst.exists) {
            val tmp = cacheFolder.getChild(
                if (dst.lcExtension.isEmpty()) dst.nameWithoutExtension + ".tmp"
                else dst.nameWithoutExtension + ".tmp.${dst.extension}"
            )
            fillFileContents(key, tmp, {
                renameTmpToDst(uuid, tmp, dst)
                data.value = load(key, dst)
            }, {
                it?.printStackTrace()
                data.value = load(key, InvalidRef)
            })
        } else {
            markUsed(uuid)
            data.value = load(key, dst)
        }
        return data
    }

    fun markUsed(uuid: String) {
        init()
        // LOGGER.info("Mark used: $uuid = $startDateTime")
        info[uuid] = startDateTime
        info.saveMaybe(configFileName)
    }

    fun renameTmpToDst(uuid: String, tmp: FileReference, dst: FileReference): Boolean {
        LastModifiedCache.invalidate(tmp)
        if (tmp.exists) {
            LastModifiedCache.invalidate(dst)
            if (dst.exists) dst.delete()
            dst.getParent().mkdirs()
            if (!tmp.renameTo(dst)) {
                LOGGER.warn("Rename from $tmp to $dst failed!")
            }
            LastModifiedCache.invalidate(dst)
            if (dst.exists) {
                markUsed(uuid)
                return true
            } else LOGGER.warn("$dst is somehow missing [1]")
        } else LOGGER.warn("$tmp is somehow missing [2]")
        return false
    }

    abstract fun fillFileContents(
        key: Key, dst: FileReference,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit,
    )

    /**
     * may return null, if the key isn't valid
     * */
    abstract fun getUniqueFilename(key: Key): String?

    private fun deleteUnusedFiles() {
        var deleted = 0
        var kept = 0
        var freed = 0L
        val children = cacheFolder.listChildren()
        for (file in children) {
            if (!file.isDirectory && abs(info[file.name, file.lastModified] - startDateTime) > FILE_TIMEOUT) {
                freed += file.length()
                file.delete()
                deleted++
            } else {
                kept++
            }
        }
        if (deleted > 0) LOGGER.info("[$name] Deleted ${deleted}/${deleted + kept} files, which haven't been used in the last 7 days. Freed ${freed.formatFileSize()}")
        info.removeIf { (_, value) -> value !is Long || abs(value - startDateTime) > FILE_TIMEOUT }
    }

    companion object {
        private const val FILE_TIMEOUT = 7 * 24 * 3600 * 1000 // delete values after one week of not-used
        private val LOGGER = LogManager.getLogger(FileCache::class)
    }
}