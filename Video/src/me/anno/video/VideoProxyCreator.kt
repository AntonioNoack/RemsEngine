package me.anno.video

import me.anno.cache.CacheData
import me.anno.cache.FileCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.Sleep.waitUntil
import me.anno.video.VideoCache.framesPerSlice
import me.anno.video.VideoCache.minSize
import me.anno.video.VideoCache.scale
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.io.MediaMetadata
import org.apache.logging.log4j.LogManager
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object VideoProxyCreator : FileCache<VideoProxyCreator.Key, FileReference>(
    "ProxyCache.json", "proxies", "VideoProxies"
) {

    data class Key(val file: FileReference, val lastModified: Long, val sliceIndex: Int)

    private fun getKey(src: FileReference, sliceIndex: Int) = Key(src, src.lastModified, sliceIndex)

    // loading is done by FFMPEG
    override fun load(key: Key, src: FileReference?): FileReference = src ?: InvalidRef

    fun getProxyFileDontUpdate(src: FileReference, sliceIndex: Int): FileReference? {
        init()
        val data = getEntryWithoutGenerator(getKey(src, sliceIndex)) as? CacheData<*>
        return data?.value as? FileReference
    }

    fun getProxyFile(src: FileReference, sliceIndex: Int, async: Boolean = true): FileReference? {
        init()
        val data = getEntry(getKey(src, sliceIndex), 10_000, async, ::generateFile) as? CacheData<*>
        return data?.value as? FileReference
    }

    override fun isKeyValid(key: Key): Boolean {
        val src1 = key.file
        return src1.exists && !src1.isDirectory
    }

    /**
     * scales video down 4x
     * */
    override fun fillFileContents(key: Key, dst: FileReference, onSuccess: () -> Unit, onError: (Exception?) -> Unit) {
        init()
        val (src, _, sliceIndex) = key
        val meta = MediaMetadata.getMeta(src, false)
        if (meta == null) {
            LOGGER.warn("Meta is null")
            onError(null)
            return
        }
        if (sliceIndex * framesPerSlice >= meta.videoFrameCount) {
            LOGGER.warn("Slice index out of bounds")
            onError(null)
            return
        }
        val w = (meta.videoWidth / scale.toFloat()).roundToInt() and (1.inv())
        val h = (meta.videoHeight / scale.toFloat()).roundToInt() and (1.inv())
        // ffmpeg -i input.avi -filter:vf scale=720:-1 -c:a copy output.mkv
        if (w < minSize || h < minSize) {
            LOGGER.warn("Size too small: ($w, $h) < $minSize")
            onError(null)
            return
        }
        dst.delete()
        object : FFMPEGStream(null, true) {
            override fun process(process: Process, vararg arguments: String) {
                // filter information, that we don't need (don't spam the console that much, rather create an overview for it)
                // devNull("error", process.errorStream)
                devLog("error", process.errorStream)
                devLog("input", process.inputStream)
                waitUntil(true) { process.waitFor(1, TimeUnit.MILLISECONDS) }
                onSuccess()
            }

            override fun destroy() {}
        }.run(
            "-y", // override existing files: they may exist, if the previous proxy creation process for this file was killed
            "-ss", "${(sliceIndex * framesPerSlice) / meta.videoFPS}", // start time
            "-i", "\"${src.absolutePath}\"",
            "-filter:v",
            "scale=\"$w:$h\"",
            "-vframes", "$framesPerSlice", // exact amount needed? (less at the end)
            "-c:a", "copy",
            dst.absolutePath
        )
    }

    override fun getUniqueFilename(key: Key): String {
        val (file, _, sliceIndex) = key
        val completePath = file.toString()
        val lastModified = file.lastModified
        return "${file.nameWithoutExtension}-" +
                "${completePath.hashCode().toUInt().toString(36)}-" +
                "${lastModified.hashCode().toUInt().toString(36)}-" +
                "$sliceIndex." +
                // just hoping the container is compatible;
                // if not, we could use the signature to figure out a compatible extension
                file.extension.ifBlank { "mp4" }
    }

    private val LOGGER = LogManager.getLogger(VideoProxyCreator::class)


}