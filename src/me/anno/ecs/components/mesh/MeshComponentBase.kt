package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

abstract class MeshComponentBase : CollidingComponent(), Renderable {

    /**
     * whether a object will receive shadows from shadow-mapped lights;
     * may be ignored for instanced meshes (to save bandwidth/computations)
     * */
    @SerializedProperty
    var receiveShadows = true

    @SerializedProperty
    var castShadows = true

    @SerializedProperty
    var isInstanced = false

    // todo respect this property
    // (useful for Synty meshes, which sometimes have awkward vertex colors)
    @SerializedProperty
    var enableVertexColors = true

    @Docs("Abstract function for you to define your mesh")
    abstract fun getMesh(): Mesh?

    @Docs("Overrides the mesh materials")
    @Type("List<Material/Reference>")
    @SerializedProperty
    var materials: List<FileReference> = emptyList()

    @Docs("For displaying random triangle colors")
    @NotSerializedProperty
    val randomTriangleId = (Maths.random() * 1e9).toInt()

    @DebugProperty
    @NotSerializedProperty
    val localAABB = AABBd()

    @DebugProperty
    @NotSerializedProperty
    val globalAABB = AABBd()

    @Docs("Ensure the mesh was loaded")
    open fun ensureBuffer() {
    }

    override fun hasRaycastType(typeMask: Int): Boolean {
        return typeMask.and(Raycast.TRIANGLES) != 0
    }

    override fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        includeDisabled: Boolean,
        result: RayHit
    ): Boolean {
        val mesh = getMesh()
        return if (mesh != null && Raycast.raycastTriangleMesh(
                entity.transform, mesh, start, direction, end, radiusAtOrigin,
                radiusPerUnit, result, typeMask
            )
        ) {
            result.mesh = mesh
            result.component = this
            true
        } else false
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateCollisionMask()
        ensureBuffer()
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        val mesh = getMesh()
        return if (mesh != null) {
            if (isInstanced && mesh.proceduralLength <= 0) {
                pipeline.addMeshInstanced(mesh, this, entity, clickId)
            } else {
                pipeline.addMesh(mesh, this, entity, gfxId)
            }
            this.clickId = clickId
            clickId + 1
        } else clickId
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        val mesh = getMesh()
        if (mesh != null) {
            val aabb2 = mesh.ensureBounds()
            localAABB.set(aabb2)
            globalAABB.clear()
            aabb2.transformUnion(globalTransform, globalAABB)
            aabb.union(globalAABB)
        }
        return true
    }

    fun fillSpace(mesh: Mesh, globalTransform: Matrix4x3d, aabb: AABBd) {
        // add aabb of that mesh with the transform
        mesh.ensureBounds().transformUnion(globalTransform, aabb)
    }

    open val hasAnimation: Boolean = false

    open fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh): Boolean {
        return false
    }

    fun draw(shader: Shader, materialIndex: Int) {
        getMesh()?.draw(shader, materialIndex)
    }

    @DebugAction
    fun printMesh() {
        val mesh = getMesh()
        if (mesh != null) {
            val pos = mesh.positions ?: return
            LOGGER.debug("Positions: " + Array(pos.size / 3) {
                val i = it * 3
                Vector3f(pos[i], pos[i + 1], pos[i + 2])
            }.joinToString())
            LOGGER.debug("Indices: ${mesh.indices?.joinToString()}")
        } else {
            LOGGER.warn("Mesh is null")
        }
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshComponentBase
        clone.materials = materials // clone list?
        clone.castShadows = castShadows
        clone.receiveShadows = receiveShadows
        clone.isInstanced = isInstanced
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MeshComponentBase::class)
    }

}