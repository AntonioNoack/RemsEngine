package me.anno.objects.cache

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
        val framesPerContainer = 16
    }

}