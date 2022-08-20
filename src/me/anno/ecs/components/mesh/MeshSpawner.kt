package me.anno.ecs.components.mesh

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.Raycast.TRIANGLES
import me.anno.gpu.pipeline.InstancedAnimStack
import me.anno.gpu.pipeline.InstancedStack
import me.anno.gpu.pipeline.InstancedStackV2
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

@Docs("Displays many meshes at once without Entities; can be used for particle systems and such")
abstract class MeshSpawner : CollidingComponent(), Renderable {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        this.clickId = clickId
        var done = forEachMeshGroupPSR { mesh, material ->
            val material2 = material ?: Mesh.defaultMaterial
            val stage = material2.pipelineStage ?: pipeline.defaultStage
            val stack = stage.instancedMeshes3.getOrPut(mesh, material2) { _, _ -> InstancedStackV2() }
            if (stack.clickIds.isEmpty() || stack.clickIds.last() != this.clickId) {
                stack.clickIds.add(stack.size)
                stack.clickIds.add(this.clickId)
            }
            stack.posSizeRot
        }
        if (!done) {
            lastStack = null
            done = forEachMeshGroup { mesh, material ->
                val material2 = material ?: Mesh.defaultMaterial
                val stage = material2.pipelineStage ?: pipeline.defaultStage
                val stack = stage.instancedMeshes2.getOrPut(mesh, material2) { mesh1, _ ->
                    if (mesh1.hasBones) InstancedAnimStack() else InstancedStack()
                }
                stack.autoClickId = this.clickId
                validateLastStack()
                lastStack = stack
                lastStackIndex = stack.size
                stack
            }
            validateLastStack()
        }
        if (!done) {
            this.pipeline = pipeline
            forEachMesh { mesh, material, transform ->

                mesh.ensureBuffer()
                transform.validate()

                // check visibility
                val bounds = tmpAABB
                bounds.set(mesh.aabb)
                bounds.transform(transform.globalTransform)

                if (pipeline.frustum.contains(bounds)) {
                    if (mesh.proceduralLength <= 0) {
                        val material2 = material ?: Mesh.defaultMaterial
                        val stage = material2.pipelineStage ?: pipeline.defaultStage
                        val stack = stage.instancedMeshes2.getOrPut(mesh, material2) { mesh1, _ ->
                            if (mesh1.hasBones) InstancedAnimStack() else InstancedStack()
                        }
                        stage.addToStack(stack, null, transform, clickId)
                    } else {
                        if (Build.isDebug && mesh.numMaterials > 1) {
                            LOGGER.warn("Procedural meshes cannot support multiple materials (in MeshSpawner)")
                        }
                        val material2 = material ?: Mesh.defaultMaterial
                        val stage = material2.pipelineStage ?: pipeline.defaultStage
                        stage.add(this, mesh, entity, 0, clickId)
                    }
                }
            }
            this.pipeline = null
        }
        return clickId + 1
    }

    private var pipeline: Pipeline? = null
    private var lastStack: InstancedStack? = null
    private var lastStackIndex = 0

    private fun validateLastStack() {
        val ls = lastStack
        if (ls != null) {
            for (j in lastStackIndex until ls.size) {
                ls.transforms[j]!!.validate()
            }
        }
        lastStack = null
    }

    private val tmpAABB = AABBd()

    fun getTransform(i: Int): Transform {
        val entity = entity
        for (j in transforms.size..i) {
            transforms.add(Transform(entity))
        }
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        val entity = entity
        for (i in transforms.size until count) {
            transforms.add(Transform(entity))
        }
    }

    override fun hasRaycastType(typeMask: Int) = (typeMask and TRIANGLES) != 0

    override fun raycast(
        entity: Entity, start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        typeMask: Int, includeDisabled: Boolean, result: RayHit
    ): Boolean {
        var hit = false
        forEachMesh { mesh, _, transform ->
            if (Raycast.raycastTriangleMesh(
                    transform, mesh, start, direction, end, radiusAtOrigin,
                    radiusPerUnit, result, typeMask
                )
            ) {
                result.mesh = mesh
                result.component = this
                hit = true
            }
        }
        return hit
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    /**
     * iterates over each mesh, which is actively visible; caller shall call transform.validate() if he needs the transform
     * */
    abstract fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit)

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMesh just will be a fallback
     * */
    open fun forEachMeshGroup(run: (Mesh, Material?) -> InstancedStack) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroup just will be a fallback;
     *
     * each element must be added as position (x,y,z), scale (1d), rotation (x,y,z,w)
     * */
    open fun forEachMeshGroupPSR(run: (Mesh, Material?) -> ExpandingFloatArray) = false

    companion object {
        private val LOGGER = LogManager.getLogger(MeshSpawner::class)
    }

}