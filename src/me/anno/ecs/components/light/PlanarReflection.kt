package me.anno.ecs.components.light

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawXYPlane
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers.pbrRenderer
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.ITexture2D
import me.anno.io.serialization.NotSerializedProperty
import me.anno.mesh.Shapes
import me.anno.utils.types.Matrices.mirror
import me.anno.utils.types.Matrices.mul2
import me.anno.utils.types.Vectors.toVector3f
import org.joml.*
import org.lwjgl.opengl.GL11C.glScissor
import kotlin.math.max
import kotlin.math.min

class PlanarReflection : LightComponentBase() {

    // todo custom mesh & shader, on which it is applied?

    // 1-2*n*nT

    @NotSerializedProperty
    var lastBuffer: ITexture2D? = null
    var samples = 1
    var usesFP = true

    // todo automatically apply deferred rendering?

    // todo when the area is small, optimize it
    // todo or maybe always use a stencil mask :)

    var clearColor = Vector3f(1f, 1f, 1f)
    val globalNormal = Vector3d()

    var bothSided = true

    var near = 0.001
    var far = 1e3

    override fun onVisibleUpdate(): Boolean {

        val instance = RenderView.currentInstance!!
        val pipeline = instance.pipeline

        pipeline.clear()

        val w = instance.w
        val h = instance.h
        val aspectRatio = w.toFloat() / h

        draw(
            pipeline, w, h,
            RenderState.cameraMatrix,
            RenderState.cameraPosition,
            RenderState.cameraRotation, RenderState.worldScale
        ) { pos, rot ->
            pipeline.frustum.definePerspective(
                near, far, RenderState.fovYRadians.toDouble(),
                w, h, aspectRatio.toDouble(),
                pos, rot
            )
        }
        return true
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        // todo if not both-sided, use half-cubes
        val mesh = Shapes.cube11Smooth
        mesh.ensureBounds()
        mesh.aabb.transformUnion(globalTransform, aabb)
        return true
    }

    fun draw(
        pipeline: Pipeline, w: Int, h: Int,
        cameraMatrix: Matrix4f, cameraPosition: Vector3d, camRotation: Quaterniond,
        worldScale: Double,
        defineFrustum: (camPosition: Vector3d, camRotation: Quaterniond) -> Unit
    ) {

        val transform = transform!!.getDrawMatrix(Engine.gameTime)
        val mirrorPosition = transform.getTranslation(tmp0d)

        // local -> global = yes, this is the correct direction
        val mirrorNormal = transform
            .transformDirection(globalNormal.set(0.0, 0.0, 1.0)) // default direction: z
            .normalize()

        // todo check whether the plane would be visible
        val isBackSide = cameraPosition.dot(mirrorNormal) - mirrorPosition.dot(mirrorNormal) < 0.0
        if (isBackSide) {
            // todo is this working? in my tests, it was always just black...
            if (bothSided) {
                mirrorNormal.mul(-1.0)
            } else {
                lastBuffer = null
                return
            }
        }

        val mirror = tmp1M.identity().mirror(mirrorPosition, mirrorNormal)

        val reflectedCameraPosition = mirror.transformPosition(tmp1d.set(cameraPosition))

        // todo correctly mirror a rotation:
        // idea:
        // first transform mirror into local space,
        // then flip z
        // then transform back into global space

        // todo this is incorrect :/
        val localRotation = transform.getUnnormalizedRotation(Quaterniond())
        val camRot = Quaterniond(localRotation).invert()
        camRot.mul(camRotation)
        camRot.z = -camRot.z
        camRot.mul(localRotation)

        val root = getRoot(Entity::class)
        pipeline.ignoredEntity = entity
        // todo define the correct frustum using the correct rotation & position
        pipeline.frustum.setToEverything(reflectedCameraPosition, camRot)
        // defineFrustum(camPos, camRot)
        // pipeline.frustum.applyTransform(mirror)
        pipeline.fill(root, reflectedCameraPosition, RenderState.worldScale)
        pipeline.planarReflections.clear()

        pipeline.disableReflectionCullingPlane()
        val reflectedMirrorPosition = mirror.transformPosition(Vector3d(mirrorPosition))
        // if(mirrorPosition.dot(cameraPosition) < 0) mirrorNormal.mul(-1.0)
        pipeline.reflectionCullingPlane.set(
            mirrorNormal,
            (mirrorNormal.dot(reflectedCameraPosition) - mirrorNormal.dot(reflectedMirrorPosition)) * worldScale
        )

        mirror.setTranslation(0.0, 0.0, 0.0)
        val camMatrix = tmp0M.set(cameraMatrix).mul2(mirror)

        // todo cut frustum into local area by bounding box
        // todo is perspective then depends on camera

        val buffer = FBStack["mirror", w, h, 4, usesFP, samples, true]
        GFXState.depthMode.use(DepthMode.CLOSER) {
            useFrame(w, h, true, buffer, pbrRenderer) {
                // clear stencil?
                buffer.clearColor(clearColor, 1f, true)
                // or GL_STENCIL_BUFFER_BIT
                // find the correct sub-frame of work: maybe we don't need to draw everything
                val aabb = findRegion(tmpAABB, cameraMatrix, transform, cameraPosition)
                if (aabb.maxZ >= 0f && aabb.minX <= 1f) {
                    // todo correct culling / bounding box calculation
                    if ((aabb.minZ <= 0f || aabb.maxZ >= 1f)) {
                        aabb.setMin(-1f, -1f, 0f)
                        aabb.setMax(+1f, +1f, 0f)
                    }
                    val x0 = max(((aabb.minX * .5f + .5f) * w).toInt(), 0)
                    val y0 = max(((aabb.minY * .5f + .5f) * h).toInt(), 0)
                    val x1 = min(((aabb.maxX * .5f + .5f) * w).toInt(), w)
                    val y1 = min(((aabb.maxY * .5f + .5f) * h).toInt(), h)
                    if (x1 > x0 && y1 > y0) {
                        GFXState.scissorTest.use(true) {
                            glScissor(x0, y0, x1 - x0, y1 - y0)
                            // buffer.clearColor(1f,0f,0f,1f)
                            pipeline.draw()
                        }
                    }
                }
            }
        }

        // todo tag: visible by mirror (vampires are not being reflected)
        lastBuffer = buffer.getTexture0()

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
                val openglSpace = cameraMatrix.transformProject(worldSpace.toVector3f(vec3f))
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
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        pipeline.planarReflections.add(this)
        return clickId // not itself clickable
    }

    override fun clone(): PlanarReflection {
        val clone = PlanarReflection()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PlanarReflection
        clone.samples = samples
        clone.usesFP = usesFP
    }

    override val className get() = "PlanarReflection"

    companion object {

        // these are vectors to avoid allocate them again and again,
        // and without need to the stack allocator
        private val tmp0d = Vector3d()
        private val tmp1d = Vector3d()
        private val tmp2d = Vector3d()
        private val tmp2f = Vector3f()
        private val tmp0M = Matrix4f()
        private val tmp1M = Matrix4d()
        private val tmpAABB = AABBf()

    }

}