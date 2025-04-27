package me.anno.openxr

import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import me.anno.gpu.RenderStep.callOnGameLoop
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.openxr.XrFovf
import org.lwjgl.openxr.XrSpaceLocation
import kotlin.math.atan
import kotlin.math.tan

class OpenXRRendering(
    val window0: OSWindow, var rv: RenderView,
    val framebuffer: Framebuffer,
    val leftTexture: Texture2D,
    val rightTexture: Texture2D,
    val depthTexture: Texture2D,
) : OpenXR(window0.pointer) {

    companion object {
        fun createProjectionFov(projectionMatrix: Matrix4f, fov: XrFovf, nearZ: Float, farZ: Float, rv: RenderView?) {
            val tanLeft = tan(fov.angleLeft())
            val tanRight = tan(fov.angleRight())
            val tanUp = tan(fov.angleUp())
            val tanDown = tan(fov.angleDown())
            Perspective.setPerspectiveVR(
                projectionMatrix, tanLeft, tanRight, tanUp, tanDown,
                nearZ, farZ, farZ <= nearZ
            )
            if (rv != null) {
                rv.fovXCenter = (tanRight + tanLeft) / (tanRight - tanLeft) + 0.5f
                rv.fovYCenter = (tanUp + tanDown) / (tanUp - tanDown) + 0.5f
                rv.fovXRadians = 2f * atan((tanRight - tanLeft) * 0.5f)
                rv.fovYRadians = 2f * atan((tanUp - tanDown) * 0.5f)
            }
        }
    }

    override fun copyToDesktopWindow(w: Int, h: Int) {
        callOnGameLoop(EngineBase.instance!!, window0)
    }

    override fun beginRenderViews() {

        val session = session ?: return
        val view0 = session.viewConfigViews[0]
        val w = view0.recommendedImageRectWidth()
        val h = view0.recommendedImageRectHeight()

        beginRenderViews(rv, w, h)
    }

    private fun defineTexture(w: Int, h: Int, ct: Texture2D, colorTexture: Int, session: Int) {
        ct.width = w
        ct.height = h
        ct.pointer = colorTexture
        ct.session = session
        ct.wasCreated = true
    }

    private fun attachTexture(target: Int, colorTexture: Int) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, target, GL_TEXTURE_2D, colorTexture, 0)
    }

    override fun setupFramebuffer(
        viewIndex: Int, width: Int, height: Int,
        colorTextureI: Int, depthTextureI: Int
    ): Framebuffer {
        if (depthTextureI < 0 && !depthTexture.isCreated()) {
            depthTexture.create(TargetType.DEPTH16)
        }
        val depthTextureI = if (depthTextureI < 0) depthTexture.pointer else depthTextureI
        val session = GFXState.session
        if (framebuffer.session != session || framebuffer.pointer == 0) {
            framebuffer.session = session
            framebuffer.pointer = glGenFramebuffers()
        }
        framebuffer.width = width
        framebuffer.height = height
        val colorTexture = if (viewIndex == 0) leftTexture else rightTexture
        defineTexture(width, height, colorTexture, colorTextureI, session)
        defineTexture(width, height, depthTexture, depthTextureI, session)
        framebuffer.bind()
        attachTexture(GL_COLOR_ATTACHMENT0, colorTextureI)
        attachTexture(GL_DEPTH_ATTACHMENT, depthTextureI)
        Framebuffer.drawBuffersN(1)
        framebuffer.checkIsComplete()
        return framebuffer
    }

    private val tmpPos = Vector3f()
    private val tmpRot = Quaternionf()

    override fun renderFrame(
        viewIndex: Int, w: Int, h: Int,
        predictedDisplayTime: Long,
        handLocations: XrSpaceLocation.Buffer?,
        colorTexture: Int, depthTexture: Int,
    ) {

        val session = session ?: return
        val view = session.views[viewIndex]
        val pose = view.pose()
        val pos = pose.`position$`()
        val rot = pose.orientation()

        tmpPos.set(pos.x(), pos.y(), pos.z())
        tmpRot.set(rot.x(), rot.y(), rot.z(), rot.w())

        createProjectionFov(rv.cameraMatrix, view.fov(), rv.near, 0f, rv)
        renderFrame(
            rv, viewIndex, 0, 0, w, h, colorTexture, depthTexture,
            tmpPos, tmpRot, rv.cameraMatrix
        )

        // todo finger tracking: render user-controlled hand
    }
}