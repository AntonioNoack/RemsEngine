package me.anno.video

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX.startDateTime
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.utils.Casting.castToLong
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

object VideoProxyCreator : CacheSection("VideoProxies") {

    const val scale = 4
    private const val minSize = 16
    const val minSizeForScaling = scale * minSize

    val configName = "ProxyCache.json"

    // todo create a metadata file, where last used (Rem's Studio starting time) is written
    val info = ConfigBasics.loadConfig(configName, StringMap(), false)

    // todo check all last instances, which can be deleted...
    private val proxyFolder = File(ConfigBasics.cacheFolder, "proxies")

    fun getProxyFile(src: File): File? {
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
        } as? CacheData<File?>
        return data?.value
    }

    fun markUsed(uuid: String) {
        info[uuid] = startDateTime
        info.saveMaybe(configName)
    }

    /**
     * scales video down 4x
     * */
    fun createProxy(src: File, dst: File, uuid: String, tmp: File, callback: () -> Unit) {
        val meta = FFMPEGMetadata.getMeta(src, false) ?: return // error
        val w = (meta.videoWidth / scale.toFloat()).roundToInt() and (1.inv())
        val h = (meta.videoHeight / scale.toFloat()).roundToInt() and (1.inv())
        // ffmpeg -i input.avi -filter:vf scale=720:-1 -c:a copy output.mkv
        if (w < minSize || h < minSize) return
        dst.parentFile?.mkdirs()
        object : FFMPEGStream(null) {
            override fun process(process: Process, arguments: List<String>) {
                // todo filter information, that we don't need...
                getOutput("Proxy", process.errorStream)
                getOutput("Proxy", process.inputStream)
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
        val lastModified = LastModifiedCache[file].second
        return "${file.nameWithoutExtension}-" +
                "${completePath.hashCode().toUInt().toString(36)}-" +
                "${lastModified.hashCode().toUInt().toString(36)}." +
                file.extension
    }

    fun deleteOldProxies() {
        info.forEach { (key, value) ->
            val file = File(proxyFolder, key)
            if (file.exists()) {
                val time = castToLong(value ?: 0L) ?: 0L
                if (abs(time - startDateTime) > proxyValidityTimeout) {
                    file.delete()
                }
            }
        }
    }

    val proxyValidityTimeout = 7 * 24 * 3600 * 1000 // delete files after one week of not-used
    private val LOGGER = LogManager.getLogger(VideoProxyCreator::class)

    init {
        deleteOldProxies()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        getProxyFile(File("C:\\Users\\Antonio\\Videos\\GodRays.mp4"))
    }

}