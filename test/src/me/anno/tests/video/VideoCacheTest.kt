package me.anno.tests.video

import me.anno.gpu.drawing.DrawTextures
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.pictures
import me.anno.video.VideoCache

/**
 * test loading video frames from cache
 * */
fun main() {
    val source = pictures.getChild("Anime/img (0).gif")
    testDrawing("VideoCacheTest") { p, canvas ->
        val meta = getMeta(source).value
        if (meta != null) {
            val numFrames = meta.videoFrameCount
            val frameIndex = (((p.windowStack.mouseX - p.x) / p.width) * numFrames).toInt()
            val frame = VideoCache.getVideoFrame(source, 3, frameIndex, 256, 30.0, 1, meta).value
            if (frame != null && frame.isCreated) {
                canvas.custom {
                    DrawTextures.drawTexture(p.x, p.y, p.width, p.height, frame)
                }
            }
        }
    }
}