package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.io.MediaMetadata
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS
import me.anno.utils.Threads
import me.anno.video.VideoCreator
import me.anno.video.formats.gpu.BlankFrameDetector

fun main() {
    OfficialExtensions.initForTests()
    val src = OS.downloads.getChild("2d/black frames sample.mp4")
    val dst = src.getSibling(src.nameWithoutExtension + "-result." + src.extension)
    val meta = MediaMetadata.getMeta(src).waitFor()!!
    val delta = 5
    val start = 2 * 60 + 59 - delta // 2:59
    val end = start + 2 * delta
    var frameIndex = (start * meta.videoFPS).toInt()
    val frameCount = (end * meta.videoFPS).toInt() - frameIndex
    val bufferSize = 64
    val fps = meta.videoFPS
    val timeout = 1000L
    HiddenOpenGLContext.createOpenGL(meta.videoWidth, meta.videoHeight)
    val fb = Framebuffer("blank", meta.videoWidth, meta.videoHeight, 1, TargetType.UInt8x4, DepthBufferType.NONE)
    VideoCreator.renderVideo(meta.videoWidth, meta.videoHeight, fps, dst, frameCount, fb, { _, callback ->
        Threads.runTaskThread("frame$frameIndex") {
            val frame = BlankFrameDetector.getFrame(src, 1, frameIndex, bufferSize, fps, timeout, meta, false)!!
            addGPUTask("blank frame detection", 1) {
                GFXState.useFrame(fb) {
                    drawTexture(0, 0, frame.width, frame.height, frame)
                }
                frameIndex++
                callback()
            }
        }
    }, null)
}