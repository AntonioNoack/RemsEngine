package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.files.thumbs.ThumbsExt.createCameraMatrix
import me.anno.io.files.thumbs.ThumbsExt.drawAssimp
import me.anno.maths.Maths.PIf
import me.anno.utils.OS.desktop
import me.anno.utils.types.Floats.toRadians
import me.anno.video.VideoBackgroundTask
import me.anno.video.VideoCreator
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import org.joml.Matrix4x3f

fun main() {

    // todo rendering with multiple samples is not working
    val w = 512
    val h = 512
    val dst = desktop.getChild("test.mp4")
    val fps = 30.0
    val numFrames = (fps * 10).toLong()
    ECSRegistry.initWithGFX()

    val cameraMatrix = createCameraMatrix(1f).rotateZ(PIf)
    val tmp = RenderView(EditorState, PlayMode.EDITING, style)
    tmp.renderMode = RenderMode.LINES
    tmp.setRenderState()

    fun createModelMatrix(frameIndex: Float): Matrix4x3f {
        val stack = Matrix4x3f()
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateY(frameIndex * 5f - 2f)
        stack.rotateX((15f).toRadians())// rotate it into a nice viewing angle
        stack.rotateY((-25f).toRadians())
        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f * 0.62f)
        return stack
    }

    val vc = VideoCreator(w, h, fps, numFrames, FFMPEGEncodingBalance.M0, FFMPEGEncodingType.DEFAULT, 25, dst)
    val vbt = object : VideoBackgroundTask(vc) {
        override fun getMotionBlurSteps(time: Double): Int = 10
        override fun getShutterPercentage(time: Double): Float = 1f
        override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
            useFrame(previewRenderer) {
                GFXState.currentBuffer.clearColor(0f, 0f, 0f, 1f, false)
                GFXState.cullMode.use(CullMode.BACK) {
                    Thumbs.sphereMesh.drawAssimp(
                        cameraMatrix,
                        createModelMatrix((time / fps).toFloat()), null,
                        useMaterials = true,
                        centerMesh = false,
                        normalizeScale = false
                    )
                }
            }
        }
    }
    vc.init()
    vbt.start() // works fine...
    GFX.workGPUTasksUntilShutdown()
}