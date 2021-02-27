package me.anno.cache.data

import me.anno.cache.instances.VideoCache
import me.anno.cache.keys.VideoFrameKey
import me.anno.studio.rems.RemsStudio.gfxSettings
import me.anno.video.FFMPEGStream
import java.io.File

class VideoData(
    val file: File, val w: Int, val h: Int,
    val scale: Int, val bufferIndex: Int,
    bufferLength: Int, val fps: Double,
    val ownsFrames: Boolean
) :
    ICacheData {

    // what about video webp? I think it's pretty rare...
    val stream = FFMPEGStream.getImageSequence(
        file, w, h, bufferIndex * bufferLength,
        if (file.name.endsWith(".webp", true)) 1 else bufferLength, fps
    ) { frame, index ->
        if (!ownsFrames) {
            // on frame, if !ownsFrames, register in VideoCache
            val localIndex = index % bufferLength
            val key = VideoFrameKey(file, scale, bufferIndex, bufferLength, localIndex, fps)
            VideoCache.override(key, frame, 5000)
        }
    }

    val frames = stream.frames

    /*init {// LayerView was not keeping its resources loaded
        if("128 per second" in file.name) LOGGER.debug("get video frames $file $w $h $index $bufferLength $fps")
    }*/

    override fun destroy() {
        //if("128 per second" in file.name) LOGGER.debug("destroy v frames $file $w $h $index $bufferLength $fps")
        if (ownsFrames) {
            stream.destroy()
        }
    }

    companion object {
        // crashes once were common
        // now that this is fixed,
        // we can use a larger buffer size like 128 instead of 16 frames
        // this is yuv (1 + 1/4 + 1/4 = 1.5) * 2 MB = 3 MB per frame
        // todo the actual usage seems to be 3x higher -> correct calculation???
        // todo maybe use rgb for yuv, just a different access method?
        // * 16 = 48 MB
        // * 128 = 200 MB
        // this is less efficient for large amounts of videos,
        // but it's better for fast loading of video, because the encoder is already loaded etc...
        val framesPerContainer get() = gfxSettings.getInt("video.frames.perContainer")
    }

}