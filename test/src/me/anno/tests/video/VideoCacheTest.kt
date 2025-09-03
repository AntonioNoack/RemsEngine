package me.anno.tests.video

import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.pictures
import me.anno.video.VideoCache

/**
 * test loading video frames from cache
 * */
fun main() {
    val source = pictures.getChild("Anime/img (0).gif")
    testDrawing("VideoCacheTest") {
        val meta = getMeta(source).value ?: return@testDrawing
        val numFrames = meta.videoFrameCount
        val frameIndex = (((it.windowStack.mouseX - it.x) / it.width) * numFrames).toInt()
        val frame = VideoCache.getVideoFrame(source, 3, frameIndex, 256, 30.0, 1, meta).value
        if (frame == null || !frame.isCreated) return@testDrawing
        drawTexture(it.x, it.y, it.width, it.height, frame)
    }
}