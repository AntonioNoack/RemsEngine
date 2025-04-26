package me.anno.tests.gfx

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.EngineBase
import me.anno.engine.Events.addEvent
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.engine.ui.vr.VRRendering
import me.anno.engine.ui.vr.VRRenderingRoutine
import me.anno.gpu.OSWindow
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.RenderStep.callOnGameLoop
import me.anno.gpu.WindowManagement
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.ui.Panel
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f

// todo bug: all complex render-modes only show the left frame...
//  why the left one and not the right one???

object VREmulator : VRRendering(), VRRenderingRoutine {

    val res = 1024
    var rv: RenderView? = null
    val framebuffer = Framebuffer("VR", res * 2, res, TargetType.UInt8x4)

    val userPosition = Vector3f(0f, 0f, -2f)

    val eyeDistHalf = 0.03f
    val eyeZ = 0.1f // todo is this the correct direction??
    val leftEyeOffset = Vector3f(-eyeDistHalf, 0f, eyeZ)
    val rightEyeOffset = Vector3f(+eyeDistHalf, 0f, eyeZ)
    val leftEyeRotation = Quaternionf()
    val rightEyeRotation = Quaternionf()

    val projection = Matrix4f()
        .setPerspective(1f, 1f, 0.01f, 100f)

    override fun startSession(window: OSWindow, rv: RenderView): Boolean {
        isActive = true
        VREmulator.rv = rv
        return true
    }

    override fun drawFrame(window: OSWindow): Boolean {
        val rv = rv!!
        beginRenderViews(rv, res, res)
        renderFrame(
            rv, 0, 0, 0, res, res, 0, 0,
            leftEyeOffset, leftEyeRotation, projection
        )
        renderFrame(
            rv, 1, res, 0, res, res, 0, 0,
            rightEyeOffset, rightEyeRotation, projection
        )
        callOnGameLoop(EngineBase.instance!!, window)
        return true
    }

    override fun setRenderView(rv: RenderView) {
        VREmulator.rv = rv
    }

    override fun accumulateViewTransforms(): Int {
        position.add(leftEyeOffset).add(userPosition)
        position.add(rightEyeOffset).add(userPosition)
        rotation.add(leftEyeRotation)
        rotation.add(rightEyeRotation)
        return 2
    }

    override fun setupFramebuffer(
        viewIndex: Int, w: Int, h: Int,
        colorTextureI: Int, depthTextureI: Int
    ): Framebuffer {
        // nothing to do here
        return framebuffer
    }

    override val leftTexture: ITexture2D? get() = framebuffer.getTexture0()
    override val rightTexture: ITexture2D? get() = framebuffer.getTexture0()
    override val leftView: Vector4f = Vector4f(2f, 1f, 0f, 0f)
    override val rightView: Vector4f = Vector4f(2f, 1f, 0.5f, 0f)
    override var isActive: Boolean = false
}

/**
 * add VR-emulator window
 * render VR like in Web onto single framebuffer to find eventual clear-issues
 * */
fun main() {

    VRRenderingRoutine.vrRoutine = VREmulator

    disableRenderDoc()

    addEvent(250) {
        // open secondary window, which shows the framebuffer directly
        // todo controls for VR positions, perspective, hands, etc...
        WindowManagement.createWindow("VR Views", object : Panel(style) {
            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.draw(x0, y0, x1, y1)
                val texture = VREmulator.framebuffer.getTexture0()
                if (texture.isCreated()) {
                    DrawTextures.drawTexture(
                        x, y, width, height, texture,
                        ignoreAlpha = true
                    )
                }
            }
        })
    }

    testSceneWithUI("VR Emulator", IcosahedronModel.createIcosphere(2))
}