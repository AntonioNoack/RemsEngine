package me.anno.cache.data

import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.studio.StudioBase
import me.anno.utils.strings.StringHelper.shorten
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import kotlin.math.max

class VideoData(
    val file: FileReference, signature: String?, val w: Int, val h: Int,
    val scale: Int, val bufferIndex: Int,
    bufferLength: Int, val fps: Double,
    originalWidth: Int, // meta?.videoWidth
    originalFPS: Double, // meta?.videoFPS ?: 0.0001
    val numTotalFramesInSrc: Int,
) : ICacheData {

    init {
        val frame0 = bufferIndex * bufferLength
        if (frame0 <= -bufferLength || frame0 >= max(1, numTotalFramesInSrc))
            LOGGER.warn(
                "Access of frames is out of bounds: $frame0/$bufferLength/${numTotalFramesInSrc} by ${
                    file.absolutePath.shorten(
                        200
                    )
                }"
            )
    }

    // what about video webp? I think it's pretty rare...
    val stream = FFMPEGStream.getImageSequence(
        file, signature, w, h, bufferIndex * bufferLength,
        if (file.name.endsWith(".webp", true)) 1 else bufferLength, fps,
        originalWidth, originalFPS,
        numTotalFramesInSrc
    )

    val frames = stream.frames

    /*init {// LayerView was not keeping its resources loaded
        if("128 per second" in file.name) LOGGER.debug("get video frames $file $w $h $index $bufferLength $fps")
    }*/

    fun getFrame(localIndex: Int, needsToBeCreated: Boolean): GPUFrame? {
        val frame = frames.getOrNull(localIndex)
        return if (!needsToBeCreated || frame?.isCreated == true) frame else null
    }

    override fun destroy() {
        //if("128 per second" in file.name) LOGGER.debug("destroy v frames $file $w $h $index $bufferLength $fps")
        stream.destroy()
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(VideoData::class)

        // crashes once were common
        // now that this is fixed,
        // we can use a larger buffer size like 128 instead of 16 frames
        // this is yuv (1 + 1/4 + 1/4 = 1.5) * 2 MB = 3 MB per frame
        // to do re-measure memory usage
        // to do the actual usage seems to be 3x higher -> correct calculation???
        // to do maybe use rgb for yuv, just a different access method?
        // * 16 = 48 MB
        // * 128 = 200 MB
        // this is less efficient for large amounts of videos,
        // but it's better for fast loading of video, because the encoder is already loaded etc...
        @JvmStatic
        val framesPerContainer get() = StudioBase.instance!!.gfxSettings.getInt("video.frames.perContainer")
    }

}