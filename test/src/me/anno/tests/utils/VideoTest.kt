package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.shader.renderer.Renderer
import me.anno.image.thumbs.ThumbsExt.createCameraMatrix
import me.anno.image.thumbs.ThumbsExt.drawAssimp
import me.anno.maths.Maths.PIf
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.gfx.initWithGFX
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.utils.Color.black
import me.anno.utils.OS.desktop
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

    OfficialExtensions.register()
    ExtensionLoader.load()

    LogManager.logAll()

    val w = 512
    val h = 512
    val samples = 8
    val dst = desktop.getChild("test.gif")
    val fps = 60.0
    val duration = 10.0
    val numFrames = (fps * duration).toInt()

    initWithGFX()

    val cameraMatrix = createCameraMatrix(1f).rotateZ(PIf)

    fun createModelMatrix(frameIndex: Float): Matrix4x3f {
        val stack = Matrix4x3f()
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateY(frameIndex * 100f - 2f)
        stack.rotateX((15f).toRadians())// rotate it into a nice viewing angle
        stack.rotateY((-25f).toRadians())
        stack.scale(0.2f)
        return stack
    }

    val mesh = flatCube.front.clone() as Mesh
    mesh.material = Material.diffuse(mediumAquamarine or black).ref
    val vc = VideoCreator(w, h, fps, numFrames, FFMPEGEncodingBalance.M0, FFMPEGEncodingType.DEFAULT, 10, true, dst)
    val vbt = object : VideoBackgroundTask(vc, samples) {
        override fun getMotionBlurSteps(time: Double): Int = 5
        override fun getShutterPercentage(time: Double): Float = 1f
        override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
            useFrame(previewRenderer) {
                GFXState.currentBuffer.clearColor(0f, 0f, 0f, 0f, false)
                GFXState.cullMode.use(CullMode.FRONT) {
                    mesh.drawAssimp(
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