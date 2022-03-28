package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugTitle
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.CollidingComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.types.AABBs.transformUnion
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

abstract class MeshBaseComponent : CollidingComponent() {

    /**
     * whether a object will receive shadows from shadow-mapped lights;
     * may be ignored for instanced meshes (to save bandwidth/computations)
     * */
    @SerializedProperty
    var receiveShadows = true

    @SerializedProperty
    var castShadows = true

    var isInstanced = false

    @Type("List<Material/Reference>")
    @DebugTitle("Overrides the original materials")
    @SerializedProperty
    var materials: List<FileReference> = emptyList()

    @NotSerializedProperty
    val randomTriangleId = (Math.random() * 1e9).toInt()

    open fun ensureBuffer() {}

    abstract fun getMesh(): Mesh?

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
    ) {
        val mesh = getMesh()
        if (mesh != null && Raycast.raycastTriangleMesh(
                entity, mesh, start, direction, end,
                radiusAtOrigin, radiusPerUnit, result
            )
        ) {
            result.mesh = mesh
            result.component = this
        }
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateCollisionMask()
        ensureBuffer()
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        val mesh = getMesh()
        if (mesh != null) {
            // add aabb of that mesh with the transform
            mesh.ensureBuffer()
            mesh.aabb.transformUnion(globalTransform, aabb)
        }
        return true
    }

    open fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {
        shader.v1b("hasAnimation", false)
    }

    fun draw(shader: Shader, materialIndex: Int) {
        getMesh()?.draw(shader, materialIndex)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshBaseComponent
        clone.materials = materials // clone list?
        clone.castShadows = castShadows
        clone.receiveShadows = receiveShadows
        clone.isInstanced = isInstanced
    }

}