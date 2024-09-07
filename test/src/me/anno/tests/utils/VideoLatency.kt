package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.workGPUTasksUntilShutdown
import me.anno.gpu.shader.renderer.Renderer
import me.anno.image.thumbs.AssetThumbHelper
import me.anno.image.thumbs.AssetThumbHelper.drawAssimp
import me.anno.maths.Maths
import me.anno.mesh.Shapes
import me.anno.tests.gfx.initWithGFX
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.utils.Color
import me.anno.utils.OS
import me.anno.utils.types.Floats.toRadians
import me.anno.video.VideoBackgroundTask
import me.anno.video.VideoCreator
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f

fun main() {

    OfficialExtensions.initForTests()

    // encode a video as quickly as possible,
    //  make the GPU-CPU communication be the bottleneck,
    //  and then optimize it
    // -> we get 1300 fps encoding speed, so good enough imo (Ryzen 5 2600)
    //    we get 1800 fps on Ryzen 9 7950x3D

    LogManager.logAll()

    val w = 16
    val h = 16
    val samples = 1
    val dst = OS.desktop.getChild("test.gif")
    val fps = 60.0
    val duration = 1000.0
    val numFrames = (fps * duration).toInt()

    initWithGFX()

    val cameraMatrix = AssetThumbHelper.createCameraMatrix(1f).rotateZ(Maths.PIf)

    fun createModelMatrix(frameIndex: Float): Matrix4x3f {
        val stack = Matrix4x3f()
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateY(frameIndex * 100f - 2f)
        stack.rotateX((15f).toRadians())// rotate it into a nice viewing angle
        stack.rotateY((-25f).toRadians())
        stack.scale(0.2f)
        return stack
    }

    val mesh = Shapes.flatCube.front.clone() as Mesh
    mesh.material = Material.diffuse(mediumAquamarine or Color.black).ref
    val vc = VideoCreator(w, h, fps, numFrames, FFMPEGEncodingBalance.M0, FFMPEGEncodingType.DEFAULT, 10, true, dst)
    val vbt = object : VideoBackgroundTask(vc, samples) {
        override fun getMotionBlurSteps(time: Double): Int = 5
        override fun getShutterPercentage(time: Double): Float = 1f
        override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
            GFXState.useFrame(Renderers.previewRenderer) {
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
    workGPUTasksUntilShutdown()
}