package me.anno.openxr

import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.RenderStep.callOnGameLoop
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.input.VROffset.additionalOffset
import me.anno.input.VROffset.additionalRotation
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.openxr.XrSpaceLocation

class OpenXRRendering(
    val window0: OSWindow, var rv: RenderView,
    val fb: Framebuffer, val ct0: Texture2D, val ct1: Texture2D, val dt: Texture2D,
) : OpenXR(window0.pointer) {

    companion object {
        private val lastPosition = Vector3d()
        private var lastAngleY = 0.0
        private val tmp = Vector3d()
    }

    override fun copyToDesktopWindow(w: Int, h: Int) {
        callOnGameLoop(EngineBase.instance!!, window0)
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

        val rt = rv.controlScheme?.rotationTargetDegrees
        val newRotationY = if (rt != null) {
            (rt.y - lastAngleY).toFloat().toRadians()
        } else 0f

        tmp.set(pos).sub(lastPosition).rotate(additionalRotation)
        rv.orbitCenter.add(tmp) // scene space
        rv.radius = 3.0 // define the general speed
        lastPosition.set(pos)

        additionalOffset
            .set(position).rotate(additionalRotation).negate()
            .add(rv.orbitCenter)

        additionalRotation.rotateY(newRotationY)

        rv.orbitRotation
            .set(additionalRotation)
            .mul(rot.x.toDouble(), rot.y.toDouble(), rot.z.toDouble(), rot.w.toDouble())

        if (rt != null) {
            rv.orbitRotation.toEulerAnglesDegrees(rt)
            lastAngleY = rt.y
        }

        // todo define camera fov for frustum based on actually used angles
        rv.editorCamera.fovY = 110f // just a guess, should be good enough
        rv.updateEditorCameraTransform()
        val skipFrame = rv.skipUpdate()
        rv.prepareDrawScene(w, h, 1f, rv.editorCamera, !skipFrame, !skipFrame)
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
        Framebuffer.drawBuffersN(1)
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
            // todo copy/transform depth to target FB
            //  (should enable proper reprojection in headset, idk if that actually works)
            rv.render(0, 0, w, h)
        }
        rv.x = ox
        rv.y = oy
        rv.width = ow
        rv.height = oh
    }

    class PrevData {
        val prevCamMatrix = Matrix4f()
        val prevCamRotation = Quaterniond()
        val prevCamPosition = Vector3d()

        fun loadPrevMatrix(rv: RenderView) {
            rv.prevCamMatrix.set(prevCamMatrix)
            rv.prevCamPosition.set(prevCamPosition)
            rv.prevCamRotation.set(prevCamRotation)
        }

        fun storePrevMatrix(rv: RenderView) {
            prevCamMatrix.set(rv.cameraMatrix)
            prevCamPosition.set(rv.cameraPosition)
            prevCamRotation.set(rv.cameraRotation)
        }
    }

    val prevData = LazyMap { _: Int -> PrevData() }

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
            .set(additionalRotation)
            .mul(rot.x().toDouble(), rot.y().toDouble(), rot.z().toDouble(), rot.w().toDouble())

        RenderState.viewIndex = viewIndex
        createProjectionFov(rv.cameraMatrix, view.fov(), rv.scaledNear.toFloat(), 0f, rv)

        // offset camera matrix by (pos - centerPos) * worldScale
        val scale = rv.worldScale.toFloat()
        val dx = pos.x() - position.x
        val dy = pos.y() - position.y
        val dz = pos.z() - position.z
        rv.cameraMatrix.translate(-dx * scale, -dy * scale, -dz * scale)
        rv.cameraMatrix.rotateInv(rv.cameraRotation)

        // no scale needed, because we're using 1:1 scale between character and world
        rv.cameraPosition.set(rv.orbitCenter).sub(dx, dy, dz)

        // reduce used VRAM, if both eyes have the same resolution
        FBStack.reset()

        rv.cameraRotation.transform(rv.cameraDirection.set(0.0, 0.0, -1.0)).normalize()
        rv.pipeline.superMaterial = rv.superMaterial.material

        val prevData = prevData[viewIndex]
        prevData.loadPrevMatrix(rv)
        setupFramebuffer(viewIndex, w, h, colorTexture, depthTexture)
        renderFrame(w, h, rv)
        val skipUpdate = rv.skipUpdate()
        if (!skipUpdate) prevData.storePrevMatrix(rv)
        RenderState.viewIndex = 0

        // not really needed
        FBStack.reset()

        // todo finger tracking: render user-controlled hand
    }
}