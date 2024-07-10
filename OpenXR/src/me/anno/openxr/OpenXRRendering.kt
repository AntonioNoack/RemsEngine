package me.anno.openxr

import me.anno.Time
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.FBStack
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
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.openxr.XrSpaceLocation
import kotlin.math.PI

class OpenXRRendering(
    val window0: OSWindow, var rv: RenderView,
    val fb: Framebuffer, val ct0: Texture2D, val ct1: Texture2D, val dt: Texture2D,
) : OpenXR(window0.pointer) {

    companion object {
        private val lastPosition = Vector3d()
        var additionalRotationY = 0.0
        val additionalOffset = Vector3d()
        private var additionalRotationYTarget = 0.0
        private var lastAngleY = 0.0
        private val tmp = Vector3d()
    }

    override fun copyToDesktopWindow(w: Int, h: Int) {
        GFX.callOnGameLoop(EngineBase.instance!!, window0)
    }

    override fun beginRenderViews() {
        super.beginRenderViews()

        val session = session ?: return
        val view0 = session.viewConfigViews[0]
        val w = view0.recommendedImageRectWidth()
        val h = view0.recommendedImageRectHeight()
        val pos = position // play space
        val rot = rotation
        rv.enableOrbiting = false

        val rt = rv.controlScheme?.rotationTarget
        if (rt != null) {
            val manualRotationY = (rt.y - lastAngleY).toRadians()
            additionalRotationYTarget += manualRotationY
        }

        tmp.set(pos).sub(lastPosition).rotateY(additionalRotationY)
        rv.orbitCenter.add(tmp) // scene space
        rv.radius = 3.0 // define the general speed
        lastPosition.set(pos)

        additionalOffset
            .set(position).rotateY(additionalRotationY).mul(-1.0)
            .add(rv.orbitCenter)

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

        // todo define camera fov for frustum based on actually used angles
        rv.editorCamera.fovY = 110f // just a guess, should be good enough
        rv.updateEditorCameraTransform()
        rv.prepareDrawScene(w, h, 1f, rv.editorCamera, true)
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
        if (fb.session != session) {
            fb.session = session
            fb.pointer = 0
        }
        fb.width = w
        fb.height = h
        if (fb.pointer == 0) {
            fb.pointer = glGenFramebuffers()
        }
        val ct = if (viewIndex == 0) ct0 else ct1
        defineTexture(w, h, ct, colorTexture, session)
        defineTexture(w, h, dt, depthTextureI, session)
        fb.bind()
        attachTexture(GL_COLOR_ATTACHMENT0, colorTexture)
        attachTexture(GL_DEPTH_ATTACHMENT, depthTextureI)
        Framebuffer.drawBuffers1(0)
        fb.checkIsComplete()
    }

    private fun renderFrame(w: Int, h: Int, rv: RenderView) {
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
        viewIndex: Int, w: Int, h: Int,
        predictedDisplayTime: Long,
        handLocations: XrSpaceLocation.Buffer?,
        colorTexture: Int, depthTexture: Int
    ) {

        val session = session ?: return
        val view = session.views[viewIndex]
        val pose = view.pose()
        val pos = pose.`position$`()
        val rot = pose.orientation()

        rv.cameraRotation
            .identity().rotateY(additionalRotationY)
            .mul(rot.x(), rot.y(), rot.z(), rot.w())

        createProjectionFov(rv.cameraMatrix, view.fov(), nearZ, 0f)
        // offset camera matrix by (pos - centerPos) * worldScale
        val scale = -rv.worldScale.toFloat() // negative for inverse
        rv.cameraMatrix.translate(
            (pos.x() - position.x) * scale,
            (pos.y() - position.y) * scale,
            (pos.z() - position.z) * scale
        )
        rv.cameraMatrix.rotateInv(rv.cameraRotation)

        // to do what's the correct eye position? xD
        // todo SSAO looks weird with Meta Quest Link:
        //  as if eyes, which are tilted to the sides, aren't tilted for that
        // todo SSR has the same issue (gold-material-override is unbearable)
        // todo lights and shadows are currently weirdly offset, too
        if (false) rv.cameraPosition.set(rv.orbitCenter).sub(
            (pos.x() - position.x) * scale,
            (pos.y() - position.y) * scale,
            (pos.z() - position.z) * scale
        )

        // reduce used VRAM, if both eyes have the same resolution
        FBStack.reset()

        rv.cameraRotation.transform(rv.cameraDirection.set(0.0, 0.0, -1.0)).normalize()
        rv.pipeline.superMaterial = rv.renderMode.superMaterial
        setupFramebuffer(viewIndex, w, h, colorTexture, depthTexture)
        renderFrame(w, h, rv)

        // not really needed
        FBStack.reset()

        // todo somehow define controller positions, and show objects there
        // todo finger tracking: render user-controlled hand
    }
}