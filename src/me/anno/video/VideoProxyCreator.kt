package me.anno.video

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX.startDateTime
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.studio.rems.RemsConfig
import me.anno.studio.rems.RemsStudio
import me.anno.utils.OS
import me.anno.utils.files.Files.formatFileSize
import org.apache.logging.log4j.LogManager
import java.io.File
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

    @JvmStatic
    fun main(args: Array<String>) {
        // test for a video file
        RemsStudio.setupNames()
        RemsConfig.init()
        getProxyFile(File(OS.videos, "GodRays.mp4"))
    }

    var isInitialized = false
    fun init() {
        if (isInitialized) return
        isInitialized = true
        info = ConfigBasics.loadConfig(configName, StringMap(), false)
        proxyFolder = File(ConfigBasics.cacheFolder, "proxies").apply { mkdirs() }
        deleteOldProxies()
    }

    private val configName = "ProxyCache.json"

    // create a metadata file, where last used (Rem's Studio starting time) is written
    lateinit var info: StringMap

    // check all last instances, which can be deleted...
    lateinit var proxyFolder: File

    fun getProxyFileDontUpdate(src: File): File? {
        init()
        val data = getEntryWithoutGenerator(LastModifiedCache[src]) as? CacheData<*>
        return data?.value as? File
    }

    fun getProxyFile(src: File): File? {
        init()
        val data = getEntry(LastModifiedCache[src], 10_000, true) {
            if (src.exists() && !src.isDirectory) {
                val uuid = getUniqueFilename(src)
                val proxyFile = File(proxyFolder, uuid)
                val data = CacheData<File?>(null)
                if (!proxyFile.exists()) {
                    createProxy(
                        src, proxyFile, uuid,
                        File(proxyFolder, proxyFile.nameWithoutExtension + ".tmp.${proxyFile.extension}")
                    ) { data.value = proxyFile }
                } else {
                    markUsed(uuid)
                    data.value = proxyFile
                }
                data
            } else CacheData<File?>(null)
        } as? CacheData<*>
        return data?.value as? File
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
    private fun createProxy(src: File, dst: File, uuid: String, tmp: File, callback: () -> Unit) {
        init()
        val meta = FFMPEGMetadata.getMeta(src, false) ?: return // error
        val w = (meta.videoWidth / scale.toFloat()).roundToInt() and (1.inv())
        val h = (meta.videoHeight / scale.toFloat()).roundToInt() and (1.inv())
        // ffmpeg -i input.avi -filter:vf scale=720:-1 -c:a copy output.mkv
        if (w < minSize || h < minSize) return
        dst.parentFile?.mkdirs()
        object : FFMPEGStream(null, true) {
            override fun process(process: Process, arguments: List<String>) {
                // filter information, that we don't need (don't spam the console that much, rather create an overview for it)
                devNull("Proxy", process.errorStream)
                devNull("Proxy", process.inputStream)
                process.waitFor()
                if (tmp.exists()) {
                    if (dst.exists()) dst.deleteRecursively()
                    tmp.renameTo(dst)
                    if (dst.exists()) {
                        markUsed(uuid)
                        callback()
                    } else LOGGER.warn("$dst is somehow missing")
                }
            }

            override fun destroy() {}
        }.run(
            listOf(
                "-i", src.absolutePath, "-filter:v", "scale=\"$w:$h\"", "-c:a", "copy", tmp.absolutePath
            )
        )
    }

    private fun getUniqueFilename(file: File): String {
        val completePath = file.toString()
        val lastModified = LastModifiedCache[file].lastModified
        return "${file.nameWithoutExtension}-" +
                "${completePath.hashCode().toUInt().toString(36)}-" +
                "${lastModified.hashCode().toUInt().toString(36)}." +
                file.extension
    }

    private fun deleteOldProxies() {
        var deleted = 0
        var kept = 0
        var freed = 0L
        proxyFolder.listFiles()?.forEach { file ->
            if (!file.isDirectory && abs(info[file.name, file.lastModified()] - startDateTime) > proxyValidityTimeout) {
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