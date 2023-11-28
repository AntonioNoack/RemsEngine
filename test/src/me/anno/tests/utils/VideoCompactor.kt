package me.anno.tests.utils

import me.anno.Engine
import me.anno.image.raw.IntImage
import me.anno.utils.Color.black
import me.anno.utils.Color.g
import me.anno.utils.Color.white
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta

fun main() {
    val source = documents.getChild("RemsStudio/Example Project/Scenes/Bad Apple.webm")
    val meta = getMeta(source, false)!!
    val w = 16
    val h = 16
    val c = 64
    FFMPEGStream.getImageSequenceCPU(
        source, null, w, h,
        0, c, 1.0 / 1.2,
        meta.videoWidth, meta.videoFPS, meta.videoFrameCount,
        {}, {
            val dst = IntImage(w * c, h, false)
            for (i in it.indices) {
                val img = it[i]
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        dst.setRGB(
                            x + i * w, y,
                            if (img.getRGB(x, y).g() > 127) white
                            else black
                        )
                    }
                }
            }
            dst
                .scaleUp(6, 6)
                .write(desktop.getChild("badApple.png"))
            Engine.requestShutdown()
        })
}