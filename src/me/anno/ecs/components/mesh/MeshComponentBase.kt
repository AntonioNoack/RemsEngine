package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
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

abstract class MeshComponentBase : CollidingComponent() {

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
    val randomTriangleId = (Math.random() * 1e9).toInt()

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
                entity, this, mesh, start, direction, end,
                radiusAtOrigin, radiusPerUnit, result
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

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        val mesh = getMesh()
        if (mesh != null) {
            fillSpace(mesh, globalTransform, aabb)
        }
        return true
    }

    fun fillSpace(mesh: Mesh, globalTransform: Matrix4x3d, aabb: AABBd) {
        // add aabb of that mesh with the transform
        mesh.ensureBuffer()
        mesh.aabb.transformUnion(globalTransform, aabb)
    }

    open fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {
        shader.v1b("hasAnimation", false)
    }

    fun draw(shader: Shader, materialIndex: Int) {
        getMesh()?.draw(shader, materialIndex)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshComponentBase
        clone.materials = materials // clone list?
        clone.castShadows = castShadows
        clone.receiveShadows = receiveShadows
        clone.isInstanced = isInstanced
    }

}