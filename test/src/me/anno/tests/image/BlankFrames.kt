package me.anno.tests.image

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.UVProjection
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.utils.OS
import me.anno.video.BlankFrameDetector
import me.anno.video.VideoCreator
import me.anno.video.ffmpeg.FFMPEGMetadata
import org.joml.Matrix4fArrayList
import kotlin.concurrent.thread

fun main() {
    val src = OS.downloads.getChild("2d/black frames sample.mp4")
    val dst = src.getSibling(src.nameWithoutExtension + "-result." + src.extension)
    val meta = FFMPEGMetadata.getMeta(src, false)!!
    val delta = 5
    val start = 2 * 60 + 59 - delta // 2:59
    val end = start + 2 * delta
    var frameIndex = (start * meta.videoFPS).toInt()
    val frameCount = (end * meta.videoFPS).toInt() - frameIndex
    val bufferSize = 64
    val fps = meta.videoFPS
    val timeout = 1000L
    HiddenOpenGLContext.createOpenGL(meta.videoWidth, meta.videoHeight)
    ShaderLib.init()
    val fb = Framebuffer("tmp", meta.videoWidth, meta.videoHeight, 1, 1, false, DepthBufferType.NONE)
    VideoCreator.renderVideo(meta.videoWidth, meta.videoHeight, fps, dst, frameCount, fb) { _, callback ->
        thread(name = "frame$frameIndex") {
            val frame = BlankFrameDetector.getFrame(src, 1, frameIndex, bufferSize, fps, timeout, meta, false)!!
            GFX.addGPUTask("bfd", 1) {
                GFXState.useFrame(fb) {
                    val stack = Matrix4fArrayList()
                    stack.scale(meta.videoHeight / meta.videoWidth.toFloat(), -1f, 1f)
                    GFXx3D.draw3D(
                        stack, frame, -1, Filtering.CUBIC, Clamping.CLAMP,
                        null, UVProjection.Planar
                    )
                }
                frameIndex++
                callback()
            }
        }
    }
}