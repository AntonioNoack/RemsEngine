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
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.*
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3d

@Docs("Displays many meshes at once without Entities; can be used for particle systems and such")
abstract class MeshSpawner : CollidingComponent(), Renderable {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    /**
     * calls forEachInstanceGroup, forEachMeshGroupI32, forEachMeshGroupTRS, forEachMeshGroup, forEachMesh,
     * until the first one of them returns true;
     * if you need different behaviour, just override this method :)
     * */
    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        this.clickId = clickId
        var done = forEachInstancedGroup { mesh, material, group, overrides ->
            val material2 = material ?: Mesh.defaultMaterial
            val stage = material2.pipelineStage ?: pipeline.defaultStage
            val stack = stage.instancedX.getOrPut(mesh, material2) { _, _ -> InstancedStackStatic.Data() }
            stack.data.add(group)
            stack.attr.add(overrides)
            stack.clickIds.add(this.clickId)
        }
        done = done || forEachMeshGroupI32 { mesh, material, matrix ->
            val material2 = material ?: Mesh.defaultMaterial
            val stage = material2.pipelineStage ?: pipeline.defaultStage
            val stack = stage.instancedI32.getOrPut(mesh, material2) { _, _ -> InstancedStackI32.Data() }
            if (stack.clickIds.isEmpty() || (stack.clickIds.last() != this.clickId || stack.matrices.last() != matrix)) {
                stack.clickIds.add(stack.size)
                stack.clickIds.add(this.clickId)
                stack.matrices.add(matrix)
            }
            stack.data
        }
        done = done || forEachMeshGroupTRS { mesh, material ->
            val material2 = material ?: Mesh.defaultMaterial
            val stage = material2.pipelineStage ?: pipeline.defaultStage
            val stack = stage.instancedPSR.getOrPut(mesh, material2) { _, _ -> InstancedStackPSR.Data() }
            if (stack.clickIds.isEmpty() || stack.clickIds.last() != this.clickId) {
                stack.clickIds.add(stack.size)
                stack.clickIds.add(this.clickId)
            }
            stack.posSizeRot
        }
        lastStack = null
        done = done || forEachMeshGroup { mesh, material ->
            val material2 = material ?: Mesh.defaultMaterial
            val stage = material2.pipelineStage ?: pipeline.defaultStage
            val stack = stage.instanced.getOrPut(mesh, material2) { mesh1, _ ->
                if (mesh1.hasBones) InstancedAnimStack() else InstancedStack()
            }
            stack.autoClickId = this.clickId
            validateLastStack()
            lastStack = stack
            lastStackIndex = stack.size
            stack
        }
        validateLastStack()
        this.pipeline = pipeline
        if (!done) forEachMesh { mesh, material, transform ->

            mesh.ensureBuffer()
            transform.validate()

            // check visibility: first transform bounds into global space, then test them
            mesh.aabb.transformUnion(transform.globalTransform, tmpAABB)
            if (pipeline.frustum.contains(tmpAABB)) {
                if (mesh.proceduralLength <= 0) {
                    val material2 = material ?: Mesh.defaultMaterial
                    val stage = material2.pipelineStage ?: pipeline.defaultStage
                    val stack = stage.instanced.getOrPut(mesh, material2) { mesh1, _ ->
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
        return clickId + 1
    }

    private var pipeline: Pipeline? = null
    private var lastStack: InstancedStack? = null
    private var lastStackIndex = 0

    /**
     * helper function for forEachMeshGroup
     * */
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
        ensureTransforms(i + 1)
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        val entity = entity
        for (i in transforms.size until count) {
            val tr = Transform()
            tr.parentEntity = entity
            transforms.add(tr)
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
     *
     * useful, if there are thousands of pre-grouped meshes with the same material; reduced overhead
     * */
    open fun forEachMeshGroup(run: (Mesh, Material?) -> InstancedStack) = false

    /**
     * iterates over each mesh group, which is actively visible;
     * if this is implemented, return true; and forEachMeshGroup just will be a fallback;
     *
     * each element must be added as position (translation; x,y,z), scale (1d), rotation (quaternion; x,y,z,w)
     *
     * useful, if there are thousands of pre-grouped meshes with the same material; and just P+R+S, no shearing, only uniform scaling; reduced overhead
     * */
    open fun forEachMeshGroupTRS(run: (Mesh, Material?) -> ExpandingFloatArray) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroupPSR just will be a fallback;
     *
     * each element must be added as StaticBuffer, and attribute map like Material.shaderOverrides,
     * which then must be interpreted in the Material.shader
     *
     * useful, if you want thousands of instanced meshes, and they don't change their transform dynamically or infrequent;
     * reduced overhead, and potentially reduced memory bandwidth;
     *
     * this is like forEachMeshGroupI32, just generalized
     * */
    open fun forEachInstancedGroup(run: (Mesh, Material?, StaticBuffer, Map<String, TypeValue>) -> Unit) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroupPSR just will be a fallback;
     *
     * each element must be added as int32, which then must be interpreted in the Material.shader
     *
     * useful, if there are thousands of pre-grouped meshes with the same material, which also are within a small grid (or similar);
     * reduced overhead and memory bandwidth
     *
     * Example Function. You can implement your own pipeline stages with customized attributes for your specialized scenarios,
     * see InstancedStackU32, if you're interested
     *
     * might be removed in the future, because you could use forEachInstancedGroup without performance loss
     * */
    open fun forEachMeshGroupI32(run: (Mesh, Material?, Matrix4x3f) -> ExpandingIntArray) = false

    companion object {
        private val LOGGER = LogManager.getLogger(MeshSpawner::class)
    }

}