package me.anno.openxr

import me.anno.Time
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.dtTo01
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.openxr.XrSpaceLocation
import kotlin.math.PI

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
    private val lastPosition = Vector3d()
    private var additionalRotationY = 0.0
    private var additionalRotationYTarget = 0.0
    private var lastAngleY = 0.0
    private val tmp = Vector3d()

    override fun beginRenderViews() {
        super.beginRenderViews()
        val session = session ?: return
        val view0 = session.viewConfigViews[0]
        val w = view0.recommendedImageRectWidth()
        val h = view0.recommendedImageRectHeight()
        val pos = position
        val rot = rotation
        rv.enableOrbiting = false

        val rt = rv.controlScheme?.rotationTarget
        if (rt != null) {
            val manualRotationY = (rt.y - lastAngleY).toRadians()
            additionalRotationYTarget += manualRotationY
        }

        tmp.set(pos).sub(lastPosition).rotateY(additionalRotationY)
        rv.orbitCenter.add(tmp)
        rv.radius = 5.0 // to define the general speed
        lastPosition.set(pos)

        // prevent 360° jumps
        var da = additionalRotationYTarget - additionalRotationY
        if (da < -PI) da += TAU
        if (da > PI) da -= TAU
        additionalRotationY += da * dtTo01(Time.deltaTime * 5.0)

        rv.orbitRotation
            .identity().rotateY(additionalRotationY)
            .mul(rot.x.toDouble(), rot.y.toDouble(), rot.z.toDouble(), rot.w.toDouble())

        if (rt != null) {
            rv.orbitRotation.toEulerAnglesDegrees(rt)
            lastAngleY = rt.y
        }

        rv.updateEditorCameraTransform()
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

        rv.cameraRotation.identity()
            .rotateY(additionalRotationY)
            .mul(rot.x().toDouble(), rot.y().toDouble(), rot.z().toDouble(), rot.w().toDouble())
            .invert()

        createProjectionFov(rv.cameraMatrix, view.fov(), nearZ, 0f)
        // offset camera matrix by (pos - centerPos) * worldScale
        val scale = -rv.worldScale.toFloat() // negative for inverse
        rv.cameraMatrix.translate(
            (pos.x() - position.x) * scale,
            (pos.y() - position.y) * scale,
            (pos.z() - position.z) * scale
        )
        rv.cameraMatrix.rotate(rv.cameraRotation)
        rv.cameraRotation.transform(rv.cameraDirection.set(0.0, 0.0, -1.0)).normalize()

        setupFramebuffer(viewIndex, w, h, colorTexture, depthTexture)
        renderFrame(w, h)

        // todo all controller inputs (2x thumbsticks, 4x trigger, ABXY, HOME)
        // todo somehow define controller positions, and show objects there
        // todo finger tracking: display user-controlled hand
    }
}