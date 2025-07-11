package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawXYPlane
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.addDefaultLightsIfRequired
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.DrawSky
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.query.GPUClockNanos
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4i
import kotlin.math.abs

class PlanarReflection : LightComponentBase(), OnDrawGUI {

    // todo support lower resolution, e.g. half

    @NotSerializedProperty
    var framebuffer0: Framebuffer? = null

    // todo render both framebuffers for VR
    @NotSerializedProperty
    var framebuffer1: Framebuffer? = null

    var samples = 1
    var usesFP = true

    val globalNormal = Vector3f()

    var bothSided = true

    var near = 0.001
    var far = 1e3

    val timer = GPUClockNanos()

    // todo everything lags behind 1 frame -> this needs to be calculated after the camera position has been calculated!!!
    override fun onUpdate() {

        lastDrawn = Time.gameTimeN

        val instance = RenderView.currentInstance ?: return
        val pipeline = instance.pipeline

        val w = instance.width
        val h = instance.height

        pipeline.ignoredComponent = this
        val frustumLen = pipeline.frustum.length
        draw(pipeline, w, h, instance.cameraMatrix, instance.cameraPosition)
        pipeline.ignoredComponent = null
        pipeline.frustum.length = frustumLen

        // restore state just in case we have multiple planes or similar
        instance.setRenderState()
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        val localBounds = if (bothSided) fullCubeBounds else halfCubeBounds
        localBounds.transformUnion(globalTransform, dstUnion)
    }

    @DebugProperty
    var isBackSide = false

    fun draw(
        pipeline: Pipeline,
        w: Int, h: Int,
        cameraMatrix0: Matrix4f,
        cameraPosition: Vector3d
    ) {
        val transform = transform!!.globalTransform
        val mirrorPosition = transform.getTranslation(tmp0d)

        // local -> global = yes, this is the correct direction
        val mirrorNormal = transform
            .transformDirection(globalNormal.set(0f, 0f, 1f)) // default direction: z
            .normalize()

        isBackSide = cameraPosition.dot(mirrorNormal) < mirrorPosition.dot(mirrorNormal)
        if (isBackSide) {
            if (bothSided) {
                mirrorNormal.mul(-1f)
            } else {
                destroyFramebuffers()
                return
            }
        }

        val mirrorMatrix = tmp1M.identity().mirror(mirrorPosition, Vector3d(mirrorNormal))

        val reflectedCameraPosition = mirrorMatrix.transformPosition(tmp1d.set(cameraPosition))
        val reflectedMirrorPosition = mirrorMatrix.transformPosition(Vector3d(mirrorPosition))
        val mirrorPos = if (isBackSide) mirrorPosition else reflectedMirrorPosition - reflectedCameraPosition
        val isPerspective = abs(cameraMatrix0.m33) < 0.5f

        mirrorMatrix.setTranslation(0.0, 0.0, 0.0)
        val cameraMatrix1 = tmp0M.set(cameraMatrix0).mul(mirrorMatrix)
            .scaleLocal(1f, -1f, 1f) // flip y, so we don't need to turn around the cull-mode
        val reflectedCameraRotation = cameraMatrix1.getNormalizedRotation(Quaternionf()).invert()

        val root = getRoot(Entity::class)
        pipeline.clear()
        // todo check that this is correct...
        pipeline.frustum.defineGenerally(cameraMatrix1, reflectedCameraPosition, reflectedCameraRotation)
        pipeline.frustum.showPlanes()

        // define last frustum plane
        pipeline.frustum.planes[pipeline.frustum.length++].set(mirrorPos, mirrorNormal)

        pipeline.fill(root)
        addDefaultLightsIfRequired(pipeline, root, null)
        // mirrors inside mirrors don't work, because we could look behind things
        pipeline.planarReflections.clear()
        pipeline.reflectionCullingPlane.set(mirrorPos, mirrorNormal) // is correct

        // set render state
        RenderState.cameraMatrix.set(cameraMatrix1)
        RenderState.cameraPosition.set(reflectedCameraPosition)
        RenderState.cameraDirection.reflect(mirrorNormal) // for sorting
        RenderState.cameraRotation.set(reflectedCameraRotation)
        RenderState.calculateDirections(isPerspective, true)

        // is that worth it?
        // todo cut frustum into local area by bounding box

        val buffer = framebuffer0 ?: Framebuffer(
            "planarReflection", w, h, samples,
            if (usesFP) TargetType.Float32x3
            else TargetType.UInt8x3, DepthBufferType.INTERNAL
        )
        framebuffer0 = buffer

        // find the correct sub-frame of work: we don't need to draw everything
        val aabb = findRegion(tmpAABB, cameraMatrix0, transform, cameraPosition)
        if (aabb.maxZ >= 0f && aabb.minZ <= 1f) {

            // todo correct culling in this case
            if ((aabb.minZ <= 0f || aabb.maxZ >= 1f)) {
                aabb.setMin(-1f, -1f, 0f)
                aabb.setMax(+1f, +1f, 0f)
            }

            val x0 = max(((aabb.minX * .5f + .5f) * w).toInt(), 0)
            val y0 = max(((aabb.minY * .5f + .5f) * h).toInt(), 0)
            val x1 = min(((aabb.maxX * .5f + .5f) * w).toInt(), w)
            val y1 = min(((aabb.maxY * .5f + .5f) * h).toInt(), h)

            if (x1 > x0 && y1 > y0) {
                bindRendering(w, h, buffer, pipeline, x0, y0, x1, y1) {
                    // todo why is the normal way to draw the sky failing its depth test?
                    clearSky(pipeline)
                    pipeline.singlePassWithSky(false)
                }
            }
        }
    }

    private fun bindRendering(
        w: Int, h: Int, buffer: Framebuffer, pipeline: Pipeline,
        x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit
    ) {
        timeRendering(className, timer) {
            useFrame(w, h, true, buffer, pbrRenderer) {
                GFXState.ditherMode.use(ditherMode) {
                    GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
                        val rectangle = Vector4i(x0, h - 1 - y1, x1 - x0, y1 - y0)
                        GFXState.scissorTest.use(rectangle, render)
                    }
                }
            }
        }
    }

    private fun findRegionI(
        x: Double, y: Double, drawTransform: Matrix4x3,
        camPosition: Vector3d, cameraMatrix: Matrix4f, aabb: AABBf
    ) {
        val vec3d = tmp2d
        val vec3f = tmp2f
        val localSpace = vec3d.set(x, y, 0.0)
        val worldSpace = drawTransform.transformPosition(localSpace)
        worldSpace.sub(camPosition)
        val openglSpace = cameraMatrix.transformProject(vec3f.set(worldSpace))
        aabb.union(openglSpace)
    }

    fun findRegion(aabb: AABBf, cameraMatrix: Matrix4f, drawTransform: Matrix4x3, camPosition: Vector3d): AABBf {
        // cam * world space * position
        aabb.clear()
        findRegionI(-1.0, -1.0, drawTransform, camPosition, cameraMatrix, aabb)
        findRegionI(-1.0, +1.0, drawTransform, camPosition, cameraMatrix, aabb)
        findRegionI(+1.0, -1.0, drawTransform, camPosition, cameraMatrix, aabb)
        findRegionI(+1.0, +1.0, drawTransform, camPosition, cameraMatrix, aabb)
        return aabb
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (all) {
            drawXYPlane(entity, 0.0)
            drawXYPlane(entity, 1.0)
            drawArrowZ(entity, 1.0, 0.0)
            if (bothSided) {
                drawXYPlane(entity, -1.0)
                drawArrowZ(entity, -1.0, 0.0)
            }
        }
    }

    val framebuffer: Framebuffer?
        get() = if (RenderState.viewIndex == 1) framebuffer1 else framebuffer0

    override fun fill(pipeline: Pipeline, transform: Transform) {
        if (framebuffer?.isCreated() == true) {
            pipeline.planarReflections.add(this)
        }
    }

    private fun destroyFramebuffers() {
        framebuffer0?.destroy()
        framebuffer0 = null
        framebuffer1?.destroy()
        framebuffer1 = null
    }

    override fun destroy() {
        super.destroy()
        destroyFramebuffers()
        timer.destroy()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PlanarReflection) return
        dst.samples = samples
        dst.usesFP = usesFP
        dst.bothSided = bothSided
        dst.near = near
        dst.far = far
    }

    companion object {

        val fullCubeBounds = AABBf(
            -1f, -1f, -1f,
            +1f, +1f, +1f,
        )

        // correct?
        val halfCubeBounds = AABBf(
            -1f, -1f, 0f,
            +1f, +1f, +1f,
        )

        // these are vectors to avoid allocate them again and again,
        // and without need to the stack allocator
        private val tmp0d = Vector3d()
        private val tmp1d = Vector3d()
        private val tmp2d = Vector3d()
        private val tmp2f = Vector3f()
        private val tmp0M = Matrix4f()
        private val tmp1M = Matrix4d()
        private val tmpAABB = AABBf()

        fun clearSky(pipeline: Pipeline) {
            renderPurely {
                DrawSky.drawSky0(pipeline)
                GFXState.currentBuffer.clearDepth()
            }
        }
    }
}