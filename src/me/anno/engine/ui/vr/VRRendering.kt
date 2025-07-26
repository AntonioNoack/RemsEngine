package me.anno.engine.ui.vr

import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

abstract class VRRendering {

    val position = Vector3f()
    val rotation = Quaternionf()

    private val lastPosition = Vector3f()
    private var lastAngleY = 0.0
    private val tmp = Vector3f()

    class PrevData {

        val prevCamMatrix = Matrix4f()
        val prevCamMatrixInv = Matrix4f()
        val prevCamRotation = Quaterniond()
        val prevCamPosition = Vector3d()

        fun loadPrevMatrix(rv: RenderView) {
            rv.prevCamMatrix.set(prevCamMatrix)
            rv.prevCamMatrixInv.set(prevCamMatrixInv)
            rv.prevCamPosition.set(prevCamPosition)
            rv.prevCamRotation.set(prevCamRotation)
        }

        fun storePrevMatrix(rv: RenderView) {
            prevCamMatrix.set(rv.cameraMatrix)
            prevCamMatrixInv.set(rv.cameraMatrixInv)
            prevCamPosition.set(rv.cameraPosition)
            prevCamRotation.set(rv.cameraRotation)
        }
    }

    val prevData = LazyMap { _: Int -> PrevData() }

    fun beginRenderViews(rv: RenderView, width: Int, height: Int) {

        position.set(0f)
        rotation.set(0f, 0f, 0f, 0f)
        val viewCount = accumulateViewTransforms()
        position.mul(1f / max(1, viewCount))
        rotation.normalize()

        if (viewCount > 0) {
            overrideRenderViewControls(rv)
            prepareRenderViewRendering(rv, width, height)
        } // else we don't want to override the standard controls
    }

    private fun overrideRenderViewControls(rv: RenderView) {
        rv.enableOrbiting = false

        val rt = rv.controlScheme?.rotationTargetDegrees
        val newRotationY = if (rt != null) {
            (rt.y - lastAngleY).toFloat().toRadians()
        } else 0f

        val delta = position.sub(lastPosition, tmp)
            .rotate(VROffset.additionalRotation)
        rv.orbitCenter.add(delta) // scene space

        rv.radius = 3f // define the general speed
        lastPosition.set(position)

        VROffset.additionalOffset
            .set(position).rotate(VROffset.additionalRotation).negate()
            .add(rv.orbitCenter)

        VROffset.additionalRotation.rotateY(newRotationY)

        rv.orbitRotation
            .set(VROffset.additionalRotation)
            .mul(rotation)

        if (rt != null) {
            rv.orbitRotation.toEulerAnglesDegrees(rt)
            lastAngleY = rt.y.toDouble()
        }

        // todo define camera fov for frustum based on actually used angles
        rv.editorCamera.fovYDegrees = 110f // just a guess, should be good enough
    }

    private fun prepareRenderViewRendering(rv: RenderView, width: Int, height: Int) {
        rv.updateEditorCameraTransform()
        val skipFrame = rv.skipUpdate()
        rv.prepareDrawScene(width, height, 1f, rv.editorCamera, !skipFrame, !skipFrame)
    }

    abstract fun accumulateViewTransforms(): Int

    fun renderFrame(
        rv: RenderView, viewIndex: Int,
        x: Int, y: Int, width: Int, height: Int,
        colorTextureI: Int, depthTextureI: Int,
        pos: Vector3f, rot: Quaternionf,
        projectionMatrix: Matrix4f
    ) {

        RenderState.viewIndex = viewIndex

        rv.cameraMatrix.set(projectionMatrix)
        rv.cameraRotation
            .set(VROffset.additionalRotation)
            .mul(rot)

        // offset camera matrix by (pos - centerPos)
        val dx = pos.x - position.x
        val dy = pos.y - position.y
        val dz = pos.z - position.z
        rv.cameraMatrix.translate(-dx, -dy, -dz)
        rv.cameraMatrix.rotateInv(rv.cameraRotation)

        // no scale needed, because we're using 1:1 scale between character and world
        rv.cameraPosition.set(rv.orbitCenter).sub(dx, dy, dz)

        // reduce used VRAM, if both eyes have the same resolution
        FBStack.reset()

        rv.cameraRotation.transform(rv.cameraDirection.set(0.0, 0.0, -1.0)).normalize()
        rv.cameraMatrix.invert(rv.cameraMatrixInv)
        rv.pipeline.superMaterial = rv.superMaterial.material

        val prevData = prevData[viewIndex]
        prevData.loadPrevMatrix(rv)
        val framebuffer = setupFramebuffer(viewIndex, width, height, colorTextureI, depthTextureI)
        renderFrame(x, y, width, height, rv, framebuffer)
        val skipUpdate = rv.skipUpdate()
        if (!skipUpdate) prevData.storePrevMatrix(rv)
        RenderState.viewIndex = 0

        // not really needed
        FBStack.reset()
    }

    abstract fun setupFramebuffer(
        viewIndex: Int, width: Int, height: Int,
        colorTextureI: Int, depthTextureI: Int
    ): Framebuffer

    fun renderFrame(
        x: Int, y: Int, width: Int, height: Int,
        rv: RenderView, framebuffer: Framebuffer
    ) {
        val ox = rv.x
        val oy = rv.y
        val ow = rv.width
        val oh = rv.height
        rv.x = x
        rv.y = y
        rv.width = width
        rv.height = height
        rv.setRenderState()
        useFrame(x, y, width, height, framebuffer) {
            // todo copy/transform depth to target FB
            //  (should enable proper reprojection in headset, idk if that actually works)
            rv.render(x, y, x + width, y + height)
        }
        rv.x = ox
        rv.y = oy
        rv.width = ow
        rv.height = oh
    }
}