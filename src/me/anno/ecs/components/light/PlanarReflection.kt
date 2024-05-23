package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawXYPlane
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.addDefaultLightsIfRequired
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix3d
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL46C.glScissor
import kotlin.math.abs

class PlanarReflection : LightComponentBase() {

    @NotSerializedProperty
    var lastBuffer: Framebuffer? = null
    var samples = 1
    var usesFP = true

    var clearColor = Vector3f(1f, 1f, 1f)
    val globalNormal = Vector3d()

    var bothSided = true

    var near = 0.001
    var far = 1e3

    // todo everything lags behind 1 frame -> this needs to be calculated after the camera position has been calculated!!!
    override fun onUpdate(): Int {

        lastDrawn = Time.gameTimeN

        val instance = RenderView.currentInstance ?: return 1
        val pipeline = instance.pipeline

        val w = instance.width
        val h = instance.height

        pipeline.ignoredComponent = this
        val frustumLen = pipeline.frustum.length
        draw(
            instance, pipeline, w, h,
            instance.cameraMatrix,
            instance.cameraPosition,
            RenderState.worldScale
        )
        pipeline.ignoredComponent = null
        pipeline.frustum.length = frustumLen

        // restore state just in case we have multiple planes or similar
        instance.setRenderState()

        return 1
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        (if (bothSided) fullCubeBounds else halfCubeBounds)
            .transformUnion(globalTransform, aabb)
        return true
    }

    fun draw(
        ci: RenderView,
        pipeline: Pipeline, w: Int, h: Int,
        cameraMatrix0: Matrix4f, cameraPosition: Vector3d, worldScale: Double
    ) {

        val transform = transform!!.getDrawMatrix(Time.gameTimeN)
        val mirrorPosition = transform.getTranslation(tmp0d)

        // local -> global = yes, this is the correct direction
        val mirrorNormal = transform
            .transformDirection(globalNormal.set(0.0, 0.0, 1.0)) // default direction: z
            .normalize()

        val isBackSide = cameraPosition.dot(mirrorNormal) - mirrorPosition.dot(mirrorNormal) < 0.0
        if (isBackSide) {
            if (bothSided) {
                mirrorNormal.mul(-1.0)
            } else {
                lastBuffer?.destroy()
                lastBuffer = null
                return
            }
        }

        val mirrorMatrix = tmp1M.identity().mirror(mirrorPosition, mirrorNormal)

        val reflectedCameraPosition = mirrorMatrix.transformPosition(tmp1d.set(cameraPosition))
        val reflectedMirrorPosition = mirrorMatrix.transformPosition(Vector3d(mirrorPosition))
        val mirrorPos = reflectedMirrorPosition - reflectedCameraPosition
        val isPerspective = abs(cameraMatrix0.m33) < 0.5f

        // better way to do this?
        // todo is this correct??
        val m0 = Matrix3d().rotate(ci.cameraRotation).mul(mirrorMatrix)
        val reflectedCameraRotation = m0.getNormalizedRotation(Quaterniond())

        val root = getRoot(Entity::class)
        pipeline.clear()
        // todo define the correct frustum using the correct rotation & position
        if (Input.isAltDown) {
            // todo why is this not working?
            // todo can we define the frustum from our matrix?
            pipeline.frustum.definePerspective(
                near, far,
                RenderState.fovYRadians.toDouble(), w, h, w.toDouble() / h.toDouble(),
                reflectedCameraPosition, reflectedCameraRotation
            )
        } else {
            pipeline.frustum.setToEverything(reflectedCameraPosition, ci.cameraRotation)
        }

        // define last frustum plane
        pipeline.frustum.planes[pipeline.frustum.length].set(mirrorPos, mirrorNormal) // todo is this correct??, scale?
        pipeline.frustum.length++
        // pipeline.frustum.showPlanes()

        pipeline.fill(root)
        addDefaultLightsIfRequired(pipeline, root, null)
        // pipeline.planarReflections.clear()
        pipeline.reflectionCullingPlane.set(mirrorPos * worldScale, mirrorNormal) // ??

        mirrorMatrix.setTranslation(0.0, 0.0, 0.0)
        val cameraMatrix1 = tmp0M.set(cameraMatrix0).mul(mirrorMatrix)
            .scaleLocal(1f, -1f, 1f) // flip y, so we don't need to turn around the cull-mode

        // set render state
        RenderState.cameraMatrix.set(cameraMatrix1)
        RenderState.cameraPosition.set(reflectedCameraPosition)
        RenderState.cameraDirection.reflect(mirrorNormal) // for sorting
        RenderState.cameraRotation.set(reflectedCameraRotation)
        RenderState.calculateDirections(isPerspective)

        // is that worth it?
        // todo cut frustum into local area by bounding box

        val buffer = lastBuffer ?: Framebuffer("planarReflection", w, h, samples, 1, usesFP, DepthBufferType.INTERNAL)
        lastBuffer = buffer

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
                GFXState.pushDrawCallName(className)
                useFrame(w, h, true, buffer, pbrRenderer) {
                    GFXState.ditherMode.use(ditherMode) {
                        GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
                            GFXState.scissorTest.use(true) {
                                glScissor(x0, h - 1 - y1, x1 - x0, y1 - y0)
                                // todo why is the normal way to draw the sky failing its depth test?
                                clearSky(pipeline)
                                pipeline.singlePassWithSky(false)
                            }
                        }
                    }
                }
                GFXState.popDrawCallName()
            }
        }
    }

    fun findRegion(aabb: AABBf, cameraMatrix: Matrix4f, drawTransform: Matrix4x3d, camPosition: Vector3d): AABBf {
        // cam * world space * position
        aabb.clear()
        val vec3d = tmp2d
        val vec3f = tmp2f
        for (x in -1..1 step 2) {
            for (y in -1..1 step 2) {
                val localSpace = vec3d.set(x.toDouble(), y.toDouble(), 0.0)
                val worldSpace = drawTransform.transformPosition(localSpace)
                worldSpace.sub(camPosition)
                val openglSpace = cameraMatrix.transformProject(vec3f.set(worldSpace))
                aabb.union(openglSpace)
            }
        }
        return aabb
    }

    override fun onDrawGUI(all: Boolean) {
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

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        pipeline.planarReflections.add(this)
        return clickId // not itself clickable
    }

    override fun destroy() {
        super.destroy()
        lastBuffer?.destroy()
        lastBuffer = null
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PlanarReflection
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
                pipeline.drawSky0()
                GFXState.currentBuffer.clearDepth()
            }
        }
    }
}