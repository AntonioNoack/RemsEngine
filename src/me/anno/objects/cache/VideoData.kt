package me.anno.objects.cache

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.video.FFMPEGStream
import java.io.File

class VideoData(file: File, w: Int, h: Int, index: Int, val fps: Double): CacheData {

    val time0 = GFX.lastTime

    // what about video webp? I think it's pretty rare...
    val stream = FFMPEGStream.getImageSequence(file, w, h, index * framesPerContainer,
        if(file.name.endsWith(".webp", true)) 1 else framesPerContainer, fps)
    val frames = stream.frames

    override fun destroy() {
        stream.destroy()
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
        val framesPerContainer = DefaultConfig["frames.perContainer", 512]
    }

}