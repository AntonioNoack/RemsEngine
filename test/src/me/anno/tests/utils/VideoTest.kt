package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.gpu.Blitting
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.workGPUTasksUntilShutdown
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.image.thumbs.AssetThumbHelper.createCameraMatrix
import me.anno.image.thumbs.AssetThumbHelper.drawAssimp
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.tests.gfx.initWithGFX
import me.anno.ui.UIColors
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toRadians
import me.anno.video.VideoBackgroundTask
import me.anno.video.VideoCreator
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f

/**
 * Easy sample to show how a video can be rendered to a file.
 *
 * With motion blur and MSAA :D,
 * and transparency, but the borders look a little too dark -> pre-multiplied issue?
 * I don't render pre-multiplied, so that's weird...
 * */

fun main() {

    OfficialExtensions.initForTests()

    LogManager.logAll()

    val w = 512
    val h = 512
    val samples = 8
    val src = res.getChild("meshes/NavMesh.fbx")
    val dst = desktop.getChild("${src.nameWithoutExtension}.gif") // todo mp3 isn't working???
    val fps = 60.0 // todo it really doesn't feel like 60fps when using gif
    val duration = 10.0
    val numFrames = (fps * duration).toInt()
    val withTransparency = false

    initWithGFX()

    val mesh = MeshCache.getEntry(src).waitFor() as Mesh
    val cameraMatrix = createCameraMatrix(1f).rotateZ(PIf)

    fun createModelMatrix(frameIndex: Float): Matrix4x3f {
        return Matrix4x3f()
            .translate(0f, 0f, -5f)
            .rotateY(frameIndex / numFrames * TAUf)
            .rotateX((15f).toRadians())// rotate it into a nice viewing angle
            .rotateY((-25f).toRadians())
    }

    val tmp = Framebuffer("tmp", w, h, TargetType.UInt8x4, DepthBufferType.INTERNAL)
    val vc = VideoCreator(
        w, h, fps, numFrames, FFMPEGEncodingBalance.M0, FFMPEGEncodingType.DEFAULT,
        24, withTransparency, dst
    )
    val vbt = object : VideoBackgroundTask(vc, samples) {
        override fun getMotionBlurSteps(time: Double): Int = 5
        override fun getShutterPercentage(time: Double): Float = 1f
        override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
            val frameIndex = (time * fps).toFloat()
            useFrame(tmp, previewRenderer) {
                GFXState.depthMode.use(DepthMode.CLOSER) {
                    val bgColor = UIColors.paleGoldenRod.withAlpha(if (withTransparency) 0f else 1f)
                    tmp.clearColor(bgColor, depth = true)
                    GFXState.cullMode.use(CullMode.FRONT) {
                        mesh.drawAssimp(
                            cameraMatrix,
                            createModelMatrix(frameIndex), null,
                            useMaterials = true,
                            centerMesh = true,
                            normalizeScale = true
                        )
                    }
                }
            }
            Blitting.copyNoAlpha(tmp.getTexture0(), true)
        }
    }
    vc.init()
    vbt.start() // works fine...
    workGPUTasksUntilShutdown()
}