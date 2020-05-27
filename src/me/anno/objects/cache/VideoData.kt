package me.anno.objects.cache

import me.anno.video.FFMPEGStream
import java.io.File

class VideoData(file: File, index: Int): CacheData {

    var fps = 24f
    val stream = FFMPEGStream.getImageSequence(file, index * framesPerContainer, framesPerContainer, fps)
    val textures = stream.frames

    override fun destroy() {
        stream.destroy()
    }

    companion object {
        val framesPerContainer = 16
    }

}