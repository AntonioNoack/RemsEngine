package me.anno.openxr

import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.openxr.XrSpaceLocation

class OpenXRRendering(
    val window0: OSWindow, val rv: RenderView,
    val fb: Framebuffer, val ct0: Texture2D, val ct1: Texture2D, val dt: Texture2D,
) : OpenXR(window0.pointer) {

    init {
        initFramebuffers()
    }

    var needsFilling = true
    override fun copyToDesktopWindow(framebuffer: Int, w: Int, h: Int) {
        GFX.callOnGameLoop(EngineBase.instance!!, window0)
        needsFilling = true
    }

    private val camera get() = rv.editorCamera

    fun updateCamera(
        px: Float, py: Float, pz: Float,
        rx: Float, ry: Float, rz: Float, rw: Float
    ) {
        rv.enableOrbiting = false
        rv.orbitCenter.set(px, py, pz)
        rv.orbitRotation.set(rx, ry, rz, rw)
        rv.updateEditorCameraTransform()
    }

    val signP = +1f
    val signR = -1f

    override fun beginRenderViews() {
        super.beginRenderViews()
        val session = session ?: return
        val view0 = session.viewConfigViews[0]
        val w = view0.recommendedImageRectWidth()
        val h = view0.recommendedImageRectHeight()
        val position = position
        val rotation = rotation
        val signR = -signR
        updateCamera(
            signP * position.x, signP * position.y, signP * position.z,
            signR * rotation.x, signR * rotation.y, signR * rotation.z, rotation.w
        )
        rv.prepareDrawScene(w, h, 1f, camera, true)
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

    private fun setupFramebuffer(viewIndex: Int, w: Int, h: Int, colorTexture: Int, depthTexture: Int) {
        if (depthTexture < 0 && !dt.isCreated()) {
            dt.create(TargetType.DEPTH16)
        }
        val depthTextureI = if (depthTexture < 0) dt.pointer else depthTexture
        val session = GFXState.session
        fb.width = w
        fb.height = h
        fb.pointer = framebuffer
        fb.session = session
        val ct = if (viewIndex == 0) ct0 else ct1
        defineTexture(w, h, ct, colorTexture, session)
        defineTexture(w, h, dt, depthTextureI, session)
        fb.bind()
        attachTexture(GL_COLOR_ATTACHMENT0, colorTexture)
        attachTexture(GL_DEPTH_ATTACHMENT, depthTextureI)
        Framebuffer.drawBuffers1(0)
        fb.checkIsComplete()
    }

    private fun renderFrame(w: Int, h: Int) {
        val ox = rv.x
        val oy = rv.y
        val ow = rv.width
        val oh = rv.height
        rv.x = 0
        rv.y = 0
        rv.width = w
        rv.height = h
        rv.setRenderState()
        useFrame(fb) {
            rv.render(0, 0, w, h)
        }
        rv.x = ox
        rv.y = oy
        rv.width = ow
        rv.height = oh
    }

    override fun renderFrame(
        viewIndex: Int, w: Int, h: Int, predictedDisplayTime: Long, handLocations: XrSpaceLocation.Buffer?,
        framebuffer: Int, colorTexture: Int, depthTexture: Int
    ) {

        val session = session ?: return
        val view = session.views[viewIndex]
        val pose = view.pose()
        val pos = pose.`position$`()
        val rot = pose.orientation()

        rv.cameraRotation.set(signR * rot.x(), signR * rot.y(), signR * rot.z(), rot.w())
        createProjectionFov(rv.cameraMatrix, view.fov(), nearZ, 0f)
        // offset camera matrix by (pos - centerPos) / worldScale
        val signP = -signP * rv.worldScale.toFloat()
        println(
            listOf(
                rv.worldScale, viewIndex,
                signP * (pos.x() - position.x),
                signP * (pos.y() - position.y),
                signP * (pos.z() - position.z),
                signR * rot.x(),
                signR * rot.y(),
                signR * rot.z(), rot.w()
            )
        )
        rv.cameraMatrix.translate(
            signP * (pos.x() - position.x),
            signP * (pos.y() - position.y),
            signP * (pos.z() - position.z)
        )
        rv.cameraMatrix.rotate(rv.cameraRotation)
        rv.cameraRotation.transform(rv.cameraDirection.set(0.0, 0.0, -1.0)).normalize()

        setupFramebuffer(viewIndex, w, h, colorTexture, depthTexture)
        renderFrame(w, h)
    }
}