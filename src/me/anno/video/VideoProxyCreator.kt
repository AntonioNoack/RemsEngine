package me.anno.video

import me.anno.Engine.startDateTime
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.files.Files.formatFileSize
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * todo for long videos it would be more sensible to only create proxies for smaller sections, e.g. 30s
 * (we know the videos -> we could even decide it on a per-video basis :))
 * */
object VideoProxyCreator : CacheSection("VideoProxies") {

    const val scale = 4
    const val minSize = 16
    const val minSizeForScaling = scale * minSize

    private const val configName = "ProxyCache.json"

    // create a metadata file, where last used (Rem's Studio starting time) is written
    lateinit var info: StringMap

    // check all last instances, which can be deleted...
    lateinit var proxyFolder: FileReference

    var isInitialized = false
    fun init() {
        if (isInitialized) return
        isInitialized = true
        info = ConfigBasics.loadConfig(configName, InvalidRef, StringMap(), false)
        proxyFolder = ConfigBasics.cacheFolder.getChild("proxies").apply { tryMkdirs() }
        deleteOldProxies()
    }

    fun getProxyFileDontUpdate(src: FileReference): FileReference? {
        init()
        val data = getEntryWithoutGenerator(src) as? CacheData<*>
        return data?.value as? FileReference
    }

    fun getProxyFile(src: FileReference): FileReference? {
        init()
        val data = getEntry(src, 10_000, true) {
            if (src.exists && !src.isDirectory) {
                val uuid = getUniqueFilename(src)
                val proxyFile = getReference(proxyFolder, uuid)
                val data = CacheData<FileReference?>(null)
                if (!proxyFile.exists) {
                    val tmp = proxyFolder.getChild(proxyFile.nameWithoutExtension + ".tmp.${proxyFile.extension}")
                    LOGGER.debug("$src -> $tmp -> $proxyFile")
                    createProxy(src, proxyFile, uuid, tmp) { data.value = proxyFile }
                } else {
                    markUsed(uuid)
                    data.value = proxyFile
                }
                data
            } else CacheData<FileReference?>(null)
        } as? CacheData<*>
        return data?.value as? FileReference
    }

    fun markUsed(uuid: String) {
        init()
        // LOGGER.info("Mark used: $uuid = $startDateTime")
        info[uuid] = startDateTime
        info.saveMaybe(configName)
    }

    /**
     * scales video down 4x
     * */
    private fun createProxy(
        src: FileReference,
        dst: FileReference,
        uuid: String,
        tmp: FileReference,
        callback: () -> Unit
    ) {
        init()
        val meta = FFMPEGMetadata.getMeta(src, false)
        if (meta == null) {
            LOGGER.warn("Meta is null")
            return
        }
        val w = (meta.videoWidth / scale.toFloat()).roundToInt() and (1.inv())
        val h = (meta.videoHeight / scale.toFloat()).roundToInt() and (1.inv())
        // ffmpeg -i input.avi -filter:vf scale=720:-1 -c:a copy output.mkv
        if (w < minSize || h < minSize) {
            LOGGER.warn("Size too small: ($w, $h) < $minSize")
            return
        }
        dst.getParent()?.tryMkdirs()
        object : FFMPEGStream(null, true) {
            override fun process(process: Process, arguments: List<String>) {
                // filter information, that we don't need (don't spam the console that much, rather create an overview for it)
                // devNull("error", process.errorStream)
                devLog("error", process.errorStream)
                devLog("input", process.inputStream)
                waitUntil(true) { !process.isAlive }
                if (tmp.exists) {
                    if (dst.exists) dst.deleteRecursively()
                    tmp.renameTo(dst)
                    if (dst.exists) {
                        markUsed(uuid)
                        callback()
                    } else LOGGER.warn("$dst is somehow missing")
                }
            }

            override fun destroy() {}
        }.run(listOf("-i", "\"${src.absolutePath}\"", "-filter:v", "scale=\"$w:$h\"", "-c:a", "copy", tmp.absolutePath))
    }

    private fun getUniqueFilename(file: FileReference): String {
        val completePath = file.toString()
        val lastModified = file.lastModified
        return "${file.nameWithoutExtension}-" +
                "${completePath.hashCode().toUInt().toString(36)}-" +
                "${lastModified.hashCode().toUInt().toString(36)}." +
                // just hoping the container is compatible;
                // if not, we could use the signature to figure out a compatible extension
                file.extension.ifBlank { "mp4" }
    }

    private fun deleteOldProxies() {
        var deleted = 0
        var kept = 0
        var freed = 0L
        proxyFolder.listChildren()?.forEach { file ->
            if (!file.isDirectory && abs(info[file.name, file.lastModified] - startDateTime) > proxyValidityTimeout) {
                freed += file.length()
                file.delete()
                deleted++
            } else {
                kept++
            }
        }
        if (deleted > 0) LOGGER.info("Deleted ${deleted}/${deleted + kept} files, which haven't been used in the last 7 days. Freed ${freed.formatFileSize()}")
        info.removeAll(info.filter { (_, value) ->
            value !is Long || abs(value - startDateTime) > proxyValidityTimeout
        }.map { it.key })
    }

    private const val proxyValidityTimeout = 7 * 24 * 3600 * 1000 // delete files after one week of not-used
    private val LOGGER = LogManager.getLogger(VideoProxyCreator::class)


}